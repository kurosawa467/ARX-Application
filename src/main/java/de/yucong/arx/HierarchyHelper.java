package de.yucong.arx;

import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.DataType;
import org.deidentifier.arx.aggregates.HierarchyBuilderIntervalBased;
import org.deidentifier.arx.aggregates.HierarchyBuilderIntervalBased.Range;
import org.deidentifier.arx.aggregates.HierarchyBuilderOrderBased;
import org.deidentifier.arx.aggregates.HierarchyBuilderRedactionBased;
import org.deidentifier.arx.aggregates.HierarchyBuilderRedactionBased.Order;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static de.yucong.arx.Application.*;

/**
 * This class provides utility methods for producing hierarchy definition for attributes
 *
 * @author Yucong Ma
 */
public class HierarchyHelper {

    /**
     * Configure hierarchy of tourney_name attribute from pre-loaded csv file
     *
     * @return the hierarchy of attribute tourney_name
     * @throws IOException when hierarchy_tourney_name.csv file cannot be found or accessed
     */
    protected static Hierarchy getHierarchyTourneyName() throws IOException {
        return Hierarchy.create(HIERARCHY_TOURNEY_NAME, StandardCharsets.UTF_8, ',');
    }

    /**
     * Configure hierarchy for the match number using order based hierarchy builder
     *
     * @return the hierarchy of attribute match_num
     * @throws IOException when wta_matches_2021.csv file cannot be found or accessed
     */
    protected static Hierarchy getHierarchyMatchNumber() throws IOException {

        int matchNumColumnIndex = 6;

        // Create order based hierarchy builder
        HierarchyBuilderOrderBased<Long> hierarchyBuilder = HierarchyBuilderOrderBased.create(DataType.INTEGER, false);

        // Define grouping fanout sizes
        hierarchyBuilder.getLevel(0).addGroup(10, DataType.INTEGER.createAggregate().createIntervalFunction());
        hierarchyBuilder.getLevel(1).addGroup(3, DataType.INTEGER.createAggregate().createIntervalFunction());
        hierarchyBuilder.getLevel(2).addGroup(4, DataType.INTEGER.createAggregate().createIntervalFunction());

        // Prepare the hierarchy builder with array of distinct match number strings
        // The index of match_num column in the header is 6
        hierarchyBuilder.prepare(getCSVColumnData(WTA_MATCHES_2021_CSV, matchNumColumnIndex));

        // Get hierarchy from hierarchy builder
        return hierarchyBuilder.build();
    }

    /**
     * Configure hierarchy for the winner id (and the winner name) using redaction based hierarchy builder
     *
     * @return the hierarchy of attribute winner_id (and winner_name)
     * @throws IOException when wta_matches_2021.csv file cannot be found or accessed
     */
    protected static Hierarchy getHierarchyWinnerIdName(int columnIndex) throws IOException {

        // Create redaction based hierarchy builder
        HierarchyBuilderRedactionBased<?> hierarchyBuilder = HierarchyBuilderRedactionBased.create(Order.RIGHT_TO_LEFT, Order.RIGHT_TO_LEFT, ' ', '*');

        // Prepare the hierarchy builder with array of distinct match number strings
        hierarchyBuilder.prepare(getCSVColumnData(WTA_MATCHES_2021_CSV, columnIndex));

        // Get hierarchy from hierarchy builder
        return hierarchyBuilder.build();
    }

    /**
     * Configure hierarchy of winner_ioc (country name) attribute from pre-loaded csv file
     *
     * @return the hierarchy of attribute winner_ioc
     * @throws IOException when hierarchy_winner_ioc.csv file cannot be found or accessed
     */
    protected static Hierarchy getHierarchyWinnerIoc() throws IOException {
        return Hierarchy.create(HIERARCHY_WINNER_IOC, StandardCharsets.UTF_8, ',');
    }

    /**
     * Configure hierarchy for the winner age using interval based hierarchy builder
     *
     * @return the hierarchy of attribute winner_age
     * @throws IOException when wta_matches_2021.csv file cannot be found or accessed
     */
    protected static Hierarchy getHierarchyWinnerAge() throws IOException {

        int winnerAgeColumnIndex = 14;

        // Create interval based hierarchy builder
        // The actual youngest winner age is 15, and the oldest winner age is 41
        HierarchyBuilderIntervalBased<Double> hierarchyBuilder = HierarchyBuilderIntervalBased.create(
                DataType.DECIMAL,
                new Range<Double>(15.0d, 15.0d, Double.MIN_VALUE / 4),
                new Range<Double>(41.0d, 41.0d, Double.MAX_VALUE / 4));

        // Define base intervals
        // The interval size is set to 2
        hierarchyBuilder.setAggregateFunction(DataType.DECIMAL.createAggregate().createIntervalFunction(true, false));
        hierarchyBuilder.addInterval(15.0d, 20.0d);

        // Define grouping fanout size
        hierarchyBuilder.getLevel(0).addGroup(5);

        // Prepare the hierarchy builder with array of distinct match number strings
        hierarchyBuilder.prepare(getCSVColumnData(WTA_MATCHES_2021_CSV, winnerAgeColumnIndex));

        // Get hierarchy from hierarchy builder
        return hierarchyBuilder.build();
    }

    /**
     * Returns an array of distinct Strings from the csv file containing data from a single column
     *
     * @param csvFilename the csv file name
     * @param columnIndex the index of the column, under which data will be read and added to the array of Strings
     * @return an array of Strings from the specified column
     * @throws IOException when csvFilename file cannot be found or accessed
     */
    private static String[] getCSVColumnData(String csvFilename, int columnIndex) throws IOException {

        // Collect strings in a set, as the prepare(data) method in the library expects an array without duplicates
        Set<String> csvColumnHashSet = new HashSet<String>();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(csvFilename));
        String nextLine = bufferedReader.readLine();

        // Skip the header string
        nextLine = bufferedReader.readLine();

        // Read csv file line by line, and collect strings in the hashset
        while (nextLine != null) {
            String[] rowStringArray = nextLine.split(",");
            csvColumnHashSet.add(rowStringArray[columnIndex]);
            nextLine = bufferedReader.readLine();
        }

        bufferedReader.close();

        // Convert hashset to array of strings
        String[] csvColumnStringArray = new String[csvColumnHashSet.size()];
        int stringArrayIndex = 0;
        for (String csvColumnString : csvColumnHashSet) {
            csvColumnStringArray[stringArrayIndex] = csvColumnString;
            stringArrayIndex++;
        }

        return csvColumnStringArray;
    }
}
