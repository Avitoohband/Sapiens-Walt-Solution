package com.walt;

import com.walt.dao.DeliveryRepository;
import com.walt.dao.DriverRepository;
import com.walt.exceptions.NoDriverFoundException;
import com.walt.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Service
public class WaltServiceImpl implements WaltService {

    private static final int MIN_DELIVERY_DISTANCE = 0;
    private static final int MAX_DELIVERY_DISTANCE = 20;

    private final DriverRepository driverRepository;
    private final DeliveryRepository deliveryRepository;

    @Autowired
    public WaltServiceImpl(DriverRepository driverRepository, DeliveryRepository deliveryRepository) {
        this.driverRepository = driverRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    public Delivery createOrderAndAssignDriver(Customer customer, Restaurant restaurant, Date deliveryTime) {
        Driver matchedDriver = findMatchDriverOrElseThrow(restaurant, deliveryTime);

        return saveDelivery(customer, restaurant, deliveryTime, matchedDriver);
    }

    @Override
    public List<DriverDistance> getDriverRankReport() {
        return toDescOrderReportList(driverRepository.findAll());
    }

    @Override
    public List<DriverDistance> getDriverRankReportByCity(City city) {
        return toDescOrderReportList(driverRepository.findAllDriversByCity(city));
    }

    private Driver findMatchDriverOrElseThrow(Restaurant restaurant, Date deliveryTime) {
        City restaurantCity = restaurant.getCity();
        return driverRepository.findAllDriversByCity(restaurantCity)
                .stream()
                .filter(availableForDeliveryAt(deliveryTime))
                .min(getComparatorForLeastBusy())
                .orElseThrow(NoDriverFoundException::new);
    }

    private static int descendingComparator(DriverDistance o1, DriverDistance o2) {
        return Double.compare(o2.getTotalDistance(), o1.getTotalDistance());
    }

    private Delivery saveDelivery(Customer customer, Restaurant restaurant, Date deliveryTime, Driver driver) {
        Delivery delivery = new Delivery(driver, restaurant, customer, deliveryTime);
        delivery.setDistance(getRandomDeliveryDistance());

        return deliveryRepository.save(delivery);
    }

    private Predicate<Driver> availableForDeliveryAt(Date deliveryTime) {

        return driver -> deliveryRepository.findAllByDriver(driver)
                .stream()
                .noneMatch(deliveryAt(deliveryTime));
    }

    private Comparator<Driver> getComparatorForLeastBusy() {
        return Comparator.comparingInt(driver -> deliveryRepository.findAllByDriver(driver).size());
    }

    private double getRandomDeliveryDistance() {
        return ThreadLocalRandom.current().nextInt(MIN_DELIVERY_DISTANCE, MAX_DELIVERY_DISTANCE + 1);
    }

    private Predicate<Delivery> deliveryAt(Date deliveryTime) {
        return delivery -> Objects.equals(delivery.getDeliveryTime(), deliveryTime);
    }

    private List<DriverDistance> toDescOrderReportList(List<Driver> drivers) {
        return drivers
                .stream()
                .map(this::toDriverDistance)
                .sorted(WaltServiceImpl::descendingComparator)
                .collect(Collectors.toList());
    }

    private DriverDistance toDriverDistance(Driver driver) {
        return new DriverDistance() {
            @Override
            public Driver getDriver() {
                return driver;
            }

            @Override
            public Double getTotalDistance() {
                return deliveryRepository.findAllByDriver(driver)
                        .stream()
                        .mapToDouble(Delivery::getDistance)
                        .sum();
            }
        };
    }
}