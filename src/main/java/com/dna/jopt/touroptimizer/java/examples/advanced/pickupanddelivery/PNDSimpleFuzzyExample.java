package com.dna.jopt.touroptimizer.java.examples.advanced.pickupanddelivery;
/*-
 * #%L
 * JOpt TourOptimizer Examples
 * %%
 * Copyright (C) 2017 - 2020 DNA Evolutions GmbH
 * %%
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt',
 * which is part of this source code package.
 * 
 * If not, see <https://www.dna-evolutions.com/agb-conditions-and-terms/>.
 * #L%
 */
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

import com.dna.jopt.framework.body.IOptimization;
import com.dna.jopt.framework.body.Optimization;
import com.dna.jopt.framework.exception.caught.InvalidLicenceException;
import com.dna.jopt.framework.outcomewrapper.IOptimizationResult;
import com.dna.jopt.member.unit.hours.IOpeningHours;
import com.dna.jopt.member.unit.hours.IWorkingHours;
import com.dna.jopt.member.unit.hours.OpeningHours;
import com.dna.jopt.member.unit.hours.WorkingHours;
import com.dna.jopt.member.unit.node.INode;
import com.dna.jopt.member.unit.node.geo.TimeWindowGeoNode;
import com.dna.jopt.member.unit.pnd.capacity.ILoadCapacity;
import com.dna.jopt.member.unit.pnd.capacity.simple.SimpleLoadCapacity;
import com.dna.jopt.member.unit.pnd.depot.node.INodeDepot;
import com.dna.jopt.member.unit.pnd.depot.node.simple.SimpleNodeDepot;
import com.dna.jopt.member.unit.pnd.depot.resource.IResourceDepot;
import com.dna.jopt.member.unit.pnd.depot.resource.simple.SimpleResourceDepot;
import com.dna.jopt.member.unit.pnd.load.ILoad;
import com.dna.jopt.member.unit.pnd.load.simple.SimpleLoad;
import com.dna.jopt.member.unit.resource.CapacityResource;
import com.dna.jopt.member.unit.resource.IResource;
import com.dna.jopt.touroptimizer.java.examples.ExampleLicenseHelper;

import tec.units.ri.quantity.Quantities;

import java.util.List;
import static java.time.Month.MARCH;
import static tec.units.ri.unit.MetricPrefix.KILO;
import static tec.units.ri.unit.Units.METRE;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;

/**
 * In this example a resource called "JackTruck" has to deliver/pickup "Apples". Some customers
 * accept partial delivery/pickup (fuzzy) and other customers only allow full pickup/delivery.
 *
 * <p>On purpose, we create a problem that is not perfectly solvable to show the behavior of fuzzy
 * visits.
 *
 * <p>Let's look at an example where nodes cannot be fully satisfied.
 *
 * <p>FUZZY: The desired Load changes. For example from 20.0 to 10.0 request. That means, 10 items
 * were delivered, but still the customer has a request of 10.0: [ Id: Apple / Type: SimpleLoad /
 * Desired Load Exchange: 20.0 / IsRequest: true / IsFuzzy: true / Priority: 1] [ Id: Apple / Type:
 * SimpleLoad / Desired Load Exchange: 10.0 / IsRequest: true / IsFuzzy: true / Priority: 1]
 *
 * <p>NO FUZZY: The desired Load is not changing. That means, 0 items were delivered, and the
 * customer still has a request of 35.0: [ Id: Apple / Type: SimpleLoad / Desired Load Exchange:
 * 35.0 / IsRequest: true / IsFuzzy: false / Priority: 1] [ Id: Apple / Type: SimpleLoad / Desired
 * Load Exchange: 35.0 / IsRequest: true / IsFuzzy: false / Priority: 1]
 *
 * @author Jens Richter
 * @version Jul 27, 2020
 * @since Jul 27, 2020
 *     <p>Example of pick up and delivery optimization problem.
 */
public class PNDSimpleFuzzyExample extends Optimization {

  /**
   * The main method.
   *
   * @param args the arguments
   * @throws InvalidLicenceException the invalid licence exception
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InterruptedException the interrupted exception
   * @throws ExecutionException the execution exception
   * @throws TimeoutException
   */
  public static void main(String[] args)
      throws InvalidLicenceException, IOException, InterruptedException, ExecutionException,
          TimeoutException {
    new PNDSimpleFuzzyExample().example();
  }

  /**
   * To string.
   *
   * @return the string
   */
  public String toString() {
    return "Example of a pick up and delivery optimization problem using fuzzy logic.";
  }

  /**
   * Example.
   *
   * @throws InvalidLicenceException the invalid licence exception
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InterruptedException the interrupted exception
   * @throws ExecutionException the execution exception
   * @throws TimeoutException
   */
  public void example()
      throws InvalidLicenceException, IOException, InterruptedException, ExecutionException,
          TimeoutException {

    // Set license via helper
    ExampleLicenseHelper.setLicense(this);

    this.setProperties();
    this.addNodes();
    this.addRes();

    // 3.) start the optimization
    CompletableFuture<IOptimizationResult> resultFuture = this.startRunAsync();

    // Subscribe to events
    subscribeToEvents(this);

    // It is important to block the call, otherwise optimization will be terminated
    IOptimizationResult result = resultFuture.get(2, TimeUnit.MINUTES);

    System.out.println(result);
  }

  /** Sets the properties. */
  public void setProperties() {

    Properties props = new Properties();
    props.setProperty("JOptExitCondition.JOptGenerationCount", "1000");
    props.setProperty("JOpt.Algorithm.PreOptimization.SA.NumIterations", "1000");

    // We have to tell the optimizer that we have an high interest in capacity planning, Default is
    // 100
    props.setProperty("JOptWeight.Capacity", "200");
    this.addElement(props);
  }

  /** Adds the res. */
  public void addRes() {

    /*
     * Setting the resource JackTruck
     */

    List<IWorkingHours> workingHours = new ArrayList<>();
    workingHours.add(
        new WorkingHours(
            ZonedDateTime.of(2020, MARCH.getValue(), 11, 8, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2020, MARCH.getValue(), 11, 20, 0, 0, 0, ZoneId.of("Europe/Berlin"))));

    Duration maxWorkingTime = Duration.ofHours(10);
    Quantity<Length> maxDistanceKmW = Quantities.getQuantity(2800.0, KILO(METRE));

    IResource truckJack =
        new CapacityResource(
            "JackTruck", 50.1167, 7.68333, maxWorkingTime, maxDistanceKmW, workingHours);

    // Setting a depot to our truck
    truckJack.setResourceDepot(this.createResourceDepot());

    // Adding the truck to our optimization
    this.addElement(truckJack);
  }

  /**
   * Creates the resource depot.
   *
   * @return the resource depot
   */
  public IResourceDepot createResourceDepot() {

    /*
     * Defining a ResourceDepot "JackTruckDepot" that can store Apples.
     *
     */

    // We can store a maximum of 10 DeliverGood on our track (assuming that no other load is
    // present)
    // Further, we start with an initial load of 5 pallets of Apples.
    ILoadCapacity appleCapacity = new SimpleLoadCapacity("Apple", 30, 10);

    // We can store a maximum of 30 apples, as we have only a single good
    IResourceDepot depot = new SimpleResourceDepot("JackTruckDepot", 30);

    // Adding the capacities to our depot
    depot.add(appleCapacity);

    return depot;
  }

  /** Adds the nodes. */
  public void addNodes() {

    List<IOpeningHours> weeklyOpeningHours = new ArrayList<>();
    weeklyOpeningHours.add(
        new OpeningHours(
            ZonedDateTime.of(2020, MARCH.getValue(), 11, 8, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2020, MARCH.getValue(), 11, 18, 0, 0, 0, ZoneId.of("Europe/Berlin"))));

    Duration visitDuration = Duration.ofMinutes(30);

    /*
     *
     *
     */

    INode job1 =
        new TimeWindowGeoNode(
            "CustomerRequestFuzzy", 50.9333, 6.95, weeklyOpeningHours, visitDuration, 1);

    job1.setNodeDepot(this.createNodeDepot("CustomerRequestFuzzyDepot", 20, true, true));
    this.addElement(job1);

    INode job2 =
        new TimeWindowGeoNode(
            "CustomerSupplyFuzzy", 50.9333, 6.95, weeklyOpeningHours, visitDuration, 1);

    job2.setNodeDepot(this.createNodeDepot("CustomerSupplyFuzzyDepot", 40, false, true));
    this.addElement(job2);

    //
    INode job3 =
        new TimeWindowGeoNode(
            "CustomerRequest", 50.9333, 6.95, weeklyOpeningHours, visitDuration, 1);

    job3.setNodeDepot(this.createNodeDepot("CustomerRequestDepot", 35, true, false));
    this.addElement(job3);

    INode job4 =
        new TimeWindowGeoNode(
            "CustomerSupply", 50.9333, 6.95, weeklyOpeningHours, visitDuration, 1);

    job4.setNodeDepot(this.createNodeDepot("CustomerRequestDepot", 35, false, false));
    this.addElement(job4);

    //

  }

  /**
   * Creates the node depot.
   *
   * @param depotId the depot id
   * @param appleCount the apple count
   * @param isRequest the is request
   * @param isFuzzy the is fuzzy
   * @return the i node depot
   */
  public INodeDepot createNodeDepot(
      String depotId, int appleCount, boolean isRequest, boolean isFuzzy) {

    ILoad appleLoad = new SimpleLoad("Apple", appleCount, isRequest, isFuzzy);

    INodeDepot customerNodeDepot = new SimpleNodeDepot(depotId);
    customerNodeDepot.add(appleLoad);

    return customerNodeDepot;
  }

  private static void subscribeToEvents(IOptimization opti) {

    // Subscribe to events
    opti.getOptimizationEvents()
        .progressSubject()
        .subscribe(
            p -> {
              System.out.println(p.getProgressString());
            });

    opti.getOptimizationEvents()
        .errorSubject()
        .subscribe(
            e -> {
              System.out.println(e.getCause() + " " + e.getCode());
            });

    opti.getOptimizationEvents()
        .warningSubject()
        .subscribe(
            w -> {
              System.out.println(w.getDescription() + w.getCode());
            });

    opti.getOptimizationEvents()
        .statusSubject()
        .subscribe(
            s -> {
              System.out.println(s.getDescription() + " " + s.getCode());
            });
  }
}
