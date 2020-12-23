package com.dna.jopt.touroptimizer.java.examples.advanced.relationship;
/*-
 * #%L
 * JOpt TourOptimizer Examples
 * %%
 * Copyright (C) 2017 - 2020 DNA Evolutions GmbH
 * %%
 * This file is subject to the terms and conditions defined in file 'src/main/resources/LICENSE.txt',
 * which is part of this repository.
 *
 * If not, see <https://www.dna-evolutions.com/>.
 * #L%
 */
import static java.time.Month.MAY;
import static tec.units.ri.unit.MetricPrefix.KILO;
import static tec.units.ri.unit.Units.METRE;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.measure.Quantity;
import javax.measure.quantity.Length;

import com.dna.jopt.framework.body.IOptimization;
import com.dna.jopt.framework.body.Optimization;
import com.dna.jopt.framework.exception.caught.InvalidLicenceException;
import com.dna.jopt.framework.outcomewrapper.IOptimizationResult;
import com.dna.jopt.member.unit.hours.IWorkingHours;
import com.dna.jopt.member.unit.hours.IOpeningHours;
import com.dna.jopt.member.unit.hours.WorkingHours;
import com.dna.jopt.member.unit.hours.OpeningHours;
import com.dna.jopt.member.unit.node.geo.TimeWindowGeoNode;
import com.dna.jopt.member.unit.relation.node2node.tempus.INode2NodeTempusRelation;
import com.dna.jopt.member.unit.relation.node2node.tempus.RelativeTimeWindow2RelatedNodeRelation;
import com.dna.jopt.member.unit.relation.node2node.visitor.INode2NodeVisitorRelation;
import com.dna.jopt.member.unit.relation.node2node.visitor.RelativeVisitor2RelatedNodeRelation;
import com.dna.jopt.member.unit.resource.CapacityResource;
import com.dna.jopt.touroptimizer.java.examples.ExampleLicenseHelper;

import tec.units.ri.quantity.Quantities;

/**
 * Example SameVisitorRelationExample. In this example two nodes should be visited by the same
 * route. Further, another relation defines a relative TimeWindow between the two nodes, effectively
 * defining the order of visitation within the route.
 *
 * @author jrich
 * @version Dec 23, 2020
 * @since Dec 23, 2020
 */
public class SameRouteRelationAndRelativeTimeWindowExample extends Optimization {

  /**
   * The main method.
   *
   * @param args the arguments
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InvalidLicenceException the invalid licence exception
   * @throws InterruptedException the interrupted exception
   * @throws ExecutionException the execution exception
   */
  public static void main(String[] args)
      throws IOException, InvalidLicenceException, InterruptedException, ExecutionException {
    new SameRouteRelationAndRelativeTimeWindowExample().example();
  }

  /**
   * To string.
   *
   * @return the string
   */
  public String toString() {
    return "Visiting two nodes in the same route in a predefined relative time window  by using relations.";
  }

  /**
   * Example.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InvalidLicenceException the invalid licence exception
   * @throws InterruptedException the interrupted exception
   * @throws ExecutionException the execution exception
   */
  public void example()
      throws IOException, InvalidLicenceException, InterruptedException, ExecutionException {

    // Set license via helper
    ExampleLicenseHelper.setLicense(this);

    // Properties!
    this.setProperties();

    this.addNodes();
    this.addResources();

    SameRouteRelationAndRelativeTimeWindowExample.attachToObservables(this);

    CompletableFuture<IOptimizationResult> resultFuture = this.startRunAsync();

    // It is important to block the call, otherwise optimization will be terminated
    System.out.println(resultFuture.get());
  }

  /** Sets the properties. */
  private void setProperties() {

    Properties props = new Properties();

    props.setProperty("JOptExitCondition.JOptGenerationCount", "2000");
    props.setProperty("JOpt.Algorithm.PreOptimization.SA.NumIterations", "10000");
    props.setProperty("JOpt.Algorithm.PreOptimization.SA.NumRepetions", "1");

    // Related properties
    props.setProperty("JOptWeight.Relationships", "100.0"); //  Default is 100.0

    props.setProperty("JOpt.NumCPUCores", "4");

    this.addElement(props);
  }

  /** Adds the resources. */
  private void addResources() {

    List<IWorkingHours> workingHours = new ArrayList<>();
    workingHours.add(
        new WorkingHours(
            ZonedDateTime.of(2020, MAY.getValue(), 6, 8, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2020, MAY.getValue(), 6, 14, 0, 0, 0, ZoneId.of("Europe/Berlin"))));

    workingHours.add(
        new WorkingHours(
            ZonedDateTime.of(2020, MAY.getValue(), 7, 8, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2020, MAY.getValue(), 7, 14, 0, 0, 0, ZoneId.of("Europe/Berlin"))));

    workingHours.add(
        new WorkingHours(
            ZonedDateTime.of(2020, MAY.getValue(), 8, 8, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2020, MAY.getValue(), 8, 14, 0, 0, 0, ZoneId.of("Europe/Berlin"))));

    Duration maxWorkingTime = Duration.ofHours(6);
    Quantity<Length> maxDistanceKmW = Quantities.getQuantity(1200.0, KILO(METRE));

    CapacityResource rep1 =
        new CapacityResource(
            "Jack", 50.775346, 6.083887, maxWorkingTime, maxDistanceKmW, workingHours);
    rep1.setCost(0, 1, 1);
    this.addElement(rep1);
  }

  /** Adds the nodes. */
  private void addNodes() {

    List<IOpeningHours> weeklyOpeningHours = new ArrayList<>();
    weeklyOpeningHours.add(
        new OpeningHours(
            ZonedDateTime.of(2020, MAY.getValue(), 6, 8, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2020, MAY.getValue(), 6, 18, 0, 0, 0, ZoneId.of("Europe/Berlin"))));

    weeklyOpeningHours.add(
        new OpeningHours(
            ZonedDateTime.of(2020, MAY.getValue(), 7, 8, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2020, MAY.getValue(), 7, 18, 0, 0, 0, ZoneId.of("Europe/Berlin"))));

    weeklyOpeningHours.add(
        new OpeningHours(
            ZonedDateTime.of(2020, MAY.getValue(), 8, 8, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2020, MAY.getValue(), 8, 18, 0, 0, 0, ZoneId.of("Europe/Berlin"))));

    Duration visitDuration = Duration.ofMinutes(150);

    // Without any relation, Aachen->Dueren->Koeln would cluster together in the same route
    // and Essen, Wupeprtal would cluster in the other route.

    // However, here we want that Aachen and Essen is visited in the same route

    // Define some nodes
    TimeWindowGeoNode koeln =
        new TimeWindowGeoNode("Koeln", 50.9333, 6.95, weeklyOpeningHours, visitDuration, 1);
    this.addElement(koeln);

    TimeWindowGeoNode essen =
        new TimeWindowGeoNode("Essen", 51.45, 7.01667, weeklyOpeningHours, visitDuration, 1);
    this.addElement(essen);

    TimeWindowGeoNode dueren =
        new TimeWindowGeoNode("Dueren", 50.8, 6.48333, weeklyOpeningHours, visitDuration, 1);
    this.addElement(dueren);

    TimeWindowGeoNode wuppertal =
        new TimeWindowGeoNode("Wuppertal", 51.2667, 7.18333, weeklyOpeningHours, visitDuration, 1);
    this.addElement(wuppertal);

    TimeWindowGeoNode aachen =
        new TimeWindowGeoNode("Aachen", 50.775346, 6.083887, weeklyOpeningHours, visitDuration, 1);
    this.addElement(aachen);

    // Create relation
    INode2NodeVisitorRelation relSameRoute = new RelativeVisitor2RelatedNodeRelation();
    relSameRoute.setMasterNode(essen);
    relSameRoute.setRelatedNode(aachen);
    relSameRoute.setIsForcedSameRoute();

    essen.addNode2NodeRelation(relSameRoute);
    aachen.addNode2NodeRelation(relSameRoute);

    // Create a relative timeWindowRelation as delta based on master node
    INode2NodeTempusRelation relRelativeTimeWindow =
        new RelativeTimeWindow2RelatedNodeRelation(Duration.ofMinutes(0), Duration.ofMinutes(1000));
    relRelativeTimeWindow.setMasterNode(essen);
    relRelativeTimeWindow.setRelatedNode(aachen);
    relRelativeTimeWindow.setTimeComparisonJuncture(true, true);

    essen.addNode2NodeRelation(relRelativeTimeWindow);
    aachen.addNode2NodeRelation(relRelativeTimeWindow);
  }

  /**
   * Attach to observables.
   *
   * @param opti the opti
   */
  private static void attachToObservables(IOptimization opti) {

    PrintStream out = System.out;

    opti.getOptimizationEvents()
        .progressSubject()
        .subscribe(p -> out.println(p.getProgressString()));

    opti.getOptimizationEvents().warningSubject().subscribe(w -> out.println(w.toString()));

    opti.getOptimizationEvents().statusSubject().subscribe(s -> out.println(s.toString()));

    opti.getOptimizationEvents().errorSubject().subscribe(e -> out.println(e.toString()));
  }
}
