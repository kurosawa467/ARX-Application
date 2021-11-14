package de.yucong.arx;

import org.deidentifier.arx.*;
import org.deidentifier.arx.criteria.KAnonymity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This class implements an application which anonymizes WTA (Women's Tennis Association) 2021 matches data.
 * This application utilizes the ARX API with the k-anonymity model.
 *
 * @author Yucong Ma
 */
public class Application {

    private static final int WINNER_ID_COLUMN_INDEX = 7;
    private static final int WINNER_NAME_COLUMN_INDEX = 10;
    protected static final String WTA_MATCHES_2021_CSV = "src/main/resources/data/wta_matches_2021.csv";
    protected static final String HIERARCHY_TOURNEY_NAME = "src/main/resources/data/hierarchy_tourney_name.csv";
    protected static final String HIERARCHY_WINNER_IOC = "src/main/resources/data/hierarchy_winner_ioc.csv";

    public static void main(String[] args) throws IOException {

        // Import data from pre-loaded csv file
        // The last parameter means that, the csv file contains a header row
        DataSource dataSource = DataSource.createCSVSource(WTA_MATCHES_2021_CSV, StandardCharsets.UTF_8, ',', true);

        // Add selected columns from the data source
        dataSource.addColumn("tourney_name", DataType.STRING);
        dataSource.addColumn("match_num", DataType.INTEGER);
        dataSource.addColumn("winner_id", DataType.INTEGER);
        dataSource.addColumn("winner_name", DataType.STRING);
        dataSource.addColumn("winner_ioc", DataType.STRING);
        dataSource.addColumn("winner_age", DataType.DECIMAL);

        // Create data object from data source
        Data data = Data.create(dataSource);

        // Get attribute hierarchies from helper methods and set attribute types
        data.getDefinition().setAttributeType("tourney_name", HierarchyHelper.getHierarchyTourneyName());
        data.getDefinition().setAttributeType("match_num", HierarchyHelper.getHierarchyMatchNumber());
        data.getDefinition().setAttributeType("winner_id", HierarchyHelper.getHierarchyWinnerIdName(WINNER_ID_COLUMN_INDEX));
        data.getDefinition().setAttributeType("winner_name", HierarchyHelper.getHierarchyWinnerIdName(WINNER_NAME_COLUMN_INDEX));
        data.getDefinition().setAttributeType("winner_ioc", HierarchyHelper.getHierarchyWinnerIoc());
        data.getDefinition().setAttributeType("winner_age", HierarchyHelper.getHierarchyWinnerAge());

        // Create an anonymizer instance and configure k-anonymity privacy model
        ARXAnonymizer arxAnonymizer = new ARXAnonymizer();
        ARXConfiguration arxConfiguration = ARXConfiguration.create();
        arxConfiguration.addPrivacyModel(new KAnonymity(4));
        arxConfiguration.setSuppressionLimit(0.02d);

        // Anonymize data set
        ARXResult arxResult = arxAnonymizer.anonymize(data, arxConfiguration);

        // Print out anonymized result
        System.out.println("Total processing time: " + arxResult.getTime() / 1000d + "s");
        System.out.println("Anonymized WTA 2021 matches data:");
        Iterator<String[]> anonymizedDataIterator = arxResult.getOutput(false).iterator();
        while (anonymizedDataIterator.hasNext()) {
            System.out.println(Arrays.toString(anonymizedDataIterator.next()));
        }
    }
}