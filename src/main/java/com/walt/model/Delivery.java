package com.walt.model;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Delivery {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @ManyToOne
    Driver driver;

    @ManyToOne
    Restaurant restaurant;

    @ManyToOne
    Customer customer;

    Date deliveryTime;
    double distance;

    public Delivery() {
    }

    public Delivery(Driver driver, Restaurant restaurant, Customer customer, Date deliveryTime) {
        this.driver = driver;
        this.restaurant = restaurant;
        this.customer = customer;
        this.deliveryTime = deliveryTime;
    }

    public Delivery(Driver driver, Restaurant restaurant, Customer customer, Date deliveryTime, int distance) {
        this(driver, restaurant, customer, deliveryTime);
        setDistance(distance);
    }

    public Long getId() {
        return id;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Date getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(Date deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Delivery)) return false;

        Delivery delivery = (Delivery) o;

        if (!driver.equals(delivery.driver)) return false;
        if (!restaurant.equals(delivery.restaurant)) return false;
        if (!customer.equals(delivery.customer)) return false;
        return deliveryTime.equals(delivery.deliveryTime);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = driver.hashCode();
        result = 31 * result + restaurant.hashCode();
        result = 31 * result + customer.hashCode();
        result = 31 * result + deliveryTime.hashCode();
        temp = Double.doubleToLongBits(distance);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
