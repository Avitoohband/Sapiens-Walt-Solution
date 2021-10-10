package com.walt;

import com.walt.dao.DeliveryRepository;
import com.walt.dao.DriverRepository;
import com.walt.exceptions.NoDriverFoundException;
import com.walt.model.*;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class WaltServiceImplTest {

    @Autowired
    private WaltService waltService;
    @MockBean
    private DriverRepository driverRepository;
    @MockBean
    private DeliveryRepository deliveryRepository;
    @Captor
    private ArgumentCaptor<Delivery> argumentCaptor;

    @Test(expected = NoDriverFoundException.class)
    public void whenNoDriverFoundInCity_shouldThrow() {
        when(driverRepository.findAllDriversByCity(any()))
                .thenReturn(Collections.emptyList());

        waltService.createOrderAndAssignDriver(new Customer(), new Restaurant(), new Date());
    }

    @Test
    public void whenAvailableDriverForDelivery_shouldCreateAndSaveDelivery() {
        City city = new City("TelAviv");
        Driver driver = new Driver("Moshe", city);
        Customer customer = new Customer("David", city, "Borochov");
        Restaurant restaurant = new Restaurant("Japan-Japan", city, "Hahagana 21");
        Date deliveryTimeNow = new Date();
        Date deliveryTimeTomorrow = tomorrow();

        Delivery deliveryNow = new Delivery(driver, restaurant, customer, deliveryTimeNow);
        Delivery expectedDelivery = new Delivery(driver, restaurant, customer, deliveryTimeTomorrow);

        when(driverRepository.findAllDriversByCity(any())).thenReturn(
                Lists.newArrayList(driver)
        );

        when(deliveryRepository.findAllByDriver(any())).thenReturn(
                Lists.newArrayList(deliveryNow)
        );

        waltService.createOrderAndAssignDriver(customer, restaurant, deliveryTimeTomorrow);

        verify(deliveryRepository).save(argumentCaptor.capture());

        Delivery actualDelivery = argumentCaptor.getValue();

        Assert.assertEquals(expectedDelivery, actualDelivery);
    }

    @Test(expected = NoDriverFoundException.class)
    public void whenAvailableDriverIsOccupiedAtAGivenTime_shouldThrow() {
        City city = new City("TelAviv");
        Driver driver = new Driver("Moshe", city);
        Customer customer = new Customer("David", city, "Borochov");
        Restaurant restaurant = new Restaurant("Japan-Japan", city, "Hahagana 21");
        Date deliveryTimeNow = new Date();

        Delivery deliveryNow = new Delivery(driver, restaurant, customer, deliveryTimeNow);

        when(driverRepository.findAllDriversByCity(any())).thenReturn(
                Lists.newArrayList(driver)
        );

        when(deliveryRepository.findAllByDriver(any())).thenReturn(
                Lists.newArrayList(deliveryNow)
        );

        waltService.createOrderAndAssignDriver(customer, restaurant, deliveryTimeNow);
    }

    @Test
    public void whenPluralAvailableDrivers_shouldReturnLeastBusy() {
        City city = new City("TelAviv");
        Driver busyDriver = new Driver("Moshe", city);
        Driver leaseBusyDriver = new Driver("David", city);
        Customer customer = new Customer("David", city, "Borochov");
        Restaurant restaurant = new Restaurant("Japan-Japan", city, "Hahagana 21");
        Date deliveryTimeNow = new Date();

        Delivery driver1Delivery1 = new Delivery(busyDriver, restaurant, customer, deliveryTimeNow);
        Delivery driver1Delivery2 = new Delivery(busyDriver, restaurant, customer, tomorrow());

        Delivery driver2Delivery1 = new Delivery(leaseBusyDriver, restaurant, customer, deliveryTimeNow);

        when(driverRepository.findAllDriversByCity(any())).thenReturn(
                Lists.newArrayList(busyDriver, leaseBusyDriver)
        );

        when(deliveryRepository.findAllByDriver(busyDriver)).thenReturn(
                Lists.newArrayList(driver1Delivery1, driver1Delivery2)
        );

        when(deliveryRepository.findAllByDriver(leaseBusyDriver)).thenReturn(
                Lists.newArrayList(driver2Delivery1)
        );

        waltService.createOrderAndAssignDriver(customer, restaurant, tomorrow());

        verify(deliveryRepository).save(argumentCaptor.capture());

        Delivery savedDelivery = argumentCaptor.getValue();

        Driver assignedDriver = savedDelivery.getDriver();

        Assert.assertEquals(assignedDriver,leaseBusyDriver);

    }

    @Test
    public void whenRankDriverIsCalled_shouldReturnDescDriverDistanceSortedList() {
        City city = new City("Jerusalem");
        Customer customer = new Customer("Daniel", city, "Hertsel 53");
        Driver driver1 = new Driver("Eli", city);
        Driver driver2 = new Driver("Dafna", city);
        Driver driver3 = new Driver("David", city);


        Restaurant restaurant = new Restaurant("Tamara", city, "Agudat Hapoel 2");
        Date deliveryTimeNow = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(deliveryTimeNow);
        c.add(Calendar.DATE, 1);
        Date deliveryTimeTomorrow = c.getTime();

        Delivery driver1Delivery1 = new Delivery(driver1, restaurant, customer, deliveryTimeNow);
        Delivery driver1Delivery2 = new Delivery(driver1, restaurant, customer, deliveryTimeTomorrow);

        Delivery driver2Delivery1 = new Delivery(driver2, restaurant, customer, deliveryTimeNow);
        Delivery driver2Delivery2 = new Delivery(driver2, restaurant, customer, deliveryTimeTomorrow);

        Delivery driver3Delivery1 = new Delivery(driver3, restaurant, customer, deliveryTimeNow);
        Delivery driver3Delivery2 = new Delivery(driver3, restaurant, customer, deliveryTimeTomorrow);

        driver1Delivery1.setDistance(10);
        driver1Delivery2.setDistance(10);

        driver2Delivery1.setDistance(5);
        driver2Delivery2.setDistance(5);

        driver3Delivery1.setDistance(2);
        driver3Delivery2.setDistance(2);

        ArrayList<Driver> allDrivers = Lists.newArrayList(driver1, driver2, driver3);

        when(driverRepository.findAll()).thenReturn(allDrivers);

        when(deliveryRepository.findAllByDriver(driver1)).thenReturn(
                Lists.newArrayList(driver1Delivery1, driver1Delivery2)
        );

        when(deliveryRepository.findAllByDriver(driver2)).thenReturn(
                Lists.newArrayList(driver2Delivery1, driver2Delivery2)
        );

        when(deliveryRepository.findAllByDriver(driver3)).thenReturn(
                Lists.newArrayList(driver3Delivery1, driver3Delivery2)
        );

        List<DriverDistance> driverRankReport = waltService.getDriverRankReport();

        List<Double> resultDistances = driverRankReport.stream()
                .map(DriverDistance::getTotalDistance)
                .collect(Collectors.toList());

        ArrayList<Double> copyResultDistances = new ArrayList<>(resultDistances);
        copyResultDistances.sort((o1, o2) -> Double.compare(o2, o1));

        Assert.assertEquals("List is not sorted!", copyResultDistances, resultDistances);

    }

    @Test
    public void whenRankDriverFromCityIsCalled_shouldReturnDescDriverDistanceSortedList() {
        City city = new City("Jerusalem");
        Customer customer = new Customer("Daniel", city, "Hertsel 53");
        Driver driver1 = new Driver("Eli", city);
        Driver driver2 = new Driver("Dafna", city);
        Driver driver3 = new Driver("David", city);

        Restaurant restaurant = new Restaurant("Tamara", city, "Agudat Hapoel 2");
        Date deliveryTimeNow = new Date();
        Date deliveryTimeTomorrow = tomorrow();

        Delivery driver1Delivery1 = new Delivery(driver1, restaurant, customer, deliveryTimeNow, 10);
        Delivery driver1Delivery2 = new Delivery(driver1, restaurant, customer, deliveryTimeTomorrow, 10);

        Delivery driver2Delivery1 = new Delivery(driver2, restaurant, customer, deliveryTimeNow, 5);
        Delivery driver2Delivery2 = new Delivery(driver2, restaurant, customer, deliveryTimeTomorrow, 5);

        Delivery driver3Delivery1 = new Delivery(driver3, restaurant, customer, deliveryTimeNow, 2);
        Delivery driver3Delivery2 = new Delivery(driver3, restaurant, customer, deliveryTimeTomorrow, 2);

        ArrayList<Driver> allDrivers = Lists.newArrayList(driver1, driver2, driver3);

        when(driverRepository.findAllDriversByCity(eq(city))).thenReturn(allDrivers);

        when(deliveryRepository.findAllByDriver(driver1)).thenReturn(
                Lists.newArrayList(driver1Delivery1, driver1Delivery2)
        );

        when(deliveryRepository.findAllByDriver(driver2)).thenReturn(
                Lists.newArrayList(driver2Delivery1, driver2Delivery2)
        );

        when(deliveryRepository.findAllByDriver(driver3)).thenReturn(
                Lists.newArrayList(driver3Delivery1, driver3Delivery2)
        );

        List<DriverDistance> driverRankReport = waltService.getDriverRankReport();

        List<Double> resultDistances = driverRankReport.stream()
                .map(DriverDistance::getTotalDistance)
                .collect(Collectors.toList());

        ArrayList<Double> copyResultDistances = new ArrayList<>(resultDistances);
        copyResultDistances.sort((o1, o2) -> Double.compare(o2, o1));

        Assert.assertEquals("List is not sorted!", copyResultDistances, resultDistances);

    }

    private static Date tomorrow() {
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, 1);
        return c.getTime();
    }
}