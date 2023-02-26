package com.udacity.vehicles.service;

import com.udacity.vehicles.client.maps.MapsClient;
import com.udacity.vehicles.client.prices.PriceClient;
import com.udacity.vehicles.domain.Location;
import com.udacity.vehicles.domain.car.Car;
import com.udacity.vehicles.domain.car.CarRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Implements the car service create, read, update or delete
 * information about vehicles, as well as gather related
 * location and price data when desired.
 */
@Slf4j
@Service
public class CarService {

    private final CarRepository repository;

    private PriceClient priceClient;

    private MapsClient mapsClient;
    private final ModelMapper mapper;


    public CarService(CarRepository repository, @Qualifier("maps") WebClient webClientMaps, @Qualifier("pricing") WebClient webClientPricing, ModelMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.mapsClient = new MapsClient(webClientMaps, mapper);
        this.priceClient = new PriceClient(webClientPricing);
    }

    /**
     * Gathers a list of all vehicles
     *
     * @return a list of all vehicles in the CarRepository
     */
    public List<Car> list() {
        List<Car> cars = repository.findAll();
        for (Car car : cars) {
            car.setPrice(priceClient.getPrice(car.getId()));
            car.setLocation(mapsClient.getAddress(car.getLocation()));
        }
        return cars;
    }

    /**
     * Gets car information by ID (or throws exception if non-existent)
     *
     * @param id the ID number of the car to gather information on
     * @return the requested car's information, including location and price
     */
    public Car findById(Long id) {
        Car car = repository.findById(id).orElse(null);

        if (car == null) {
            log.error("Car Not Found Exception");
            throw new CarNotFoundException();
        }

        String price = priceClient.getPrice(car.getId());
        car.setPrice(price);


        Location location = mapsClient.getAddress(car.getLocation());
        car.setLocation(location);

        return car;
    }

    /**
     * Either creates or updates a vehicle, based on prior existence of car
     *
     * @param car A car object, which can be either new or existing
     * @return the new/updated car is stored in the repository
     */
    public Car save(Car car) {
        if (car.getId() != null) {
            return repository.findById(car.getId())
                    .map(carToBeUpdated -> {
                        carToBeUpdated.setDetails(car.getDetails());
                        carToBeUpdated.setLocation(car.getLocation());
                        carToBeUpdated.setCondition(car.getCondition());
                        return repository.save(carToBeUpdated);
                    }).orElseThrow(CarNotFoundException::new);
        }

        return repository.save(car);
    }

    /**
     * Deletes a given car by ID
     *
     * @param id the ID number of the car to delete
     */
    public void delete(Long id) {
        Car car = repository.findById(id).orElse(null);

        if (car == null) {
            log.error("Car Not Found Exception");
            throw new CarNotFoundException();
        }

        repository.delete(car);

    }
}
