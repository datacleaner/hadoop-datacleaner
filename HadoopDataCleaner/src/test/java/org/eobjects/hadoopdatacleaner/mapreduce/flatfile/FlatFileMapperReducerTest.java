package org.eobjects.hadoopdatacleaner.mapreduce.flatfile;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SortedMapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mrunit.mapreduce.MapDriver;
import org.apache.hadoop.mrunit.mapreduce.MapReduceDriver;
import org.apache.hadoop.mrunit.mapreduce.ReduceDriver;
import org.eobjects.analyzer.beans.StringAnalyzer;
import org.eobjects.analyzer.beans.transform.ConcatenatorTransformer;
import org.eobjects.analyzer.beans.transform.TokenizerTransformer;
import org.eobjects.analyzer.beans.valuedist.ValueDistributionAnalyzer;
import org.eobjects.analyzer.beans.writers.InsertIntoTableAnalyzer;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfigurationImpl;
import org.eobjects.analyzer.connection.CsvDatastore;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.connection.DatastoreCatalog;
import org.eobjects.analyzer.connection.DatastoreCatalogImpl;
import org.eobjects.analyzer.descriptors.Descriptors;
import org.eobjects.analyzer.descriptors.SimpleDescriptorProvider;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.builder.AnalysisJobBuilder;
import org.eobjects.analyzer.job.builder.AnalyzerJobBuilder;
import org.eobjects.analyzer.job.builder.TransformerJobBuilder;
import org.eobjects.hadoopdatacleaner.FlatFileTool;
import org.eobjects.hadoopdatacleaner.configuration.ConfigurationSerializer;
import org.eobjects.metamodel.csv.CsvConfiguration;
import org.eobjects.metamodel.util.FileResource;
import org.junit.Before;
import org.junit.Test;

public class FlatFileMapperReducerTest {

    private static final String CSV_FILE_PATH = "src/test/resources/countrycodes.csv";

    MapDriver<LongWritable, Text, LongWritable, SortedMapWritable> mapDriver;
    ReduceDriver<LongWritable, SortedMapWritable, LongWritable, Text> reduceDriver;
    MapReduceDriver<LongWritable, Text, LongWritable, SortedMapWritable, LongWritable, Text> mapReduceDriver;

    @Before
    public void setUp() {
        AnalyzerBeansConfiguration analyzerBeansConfiguration = buildAnalyzerBeansConfigurationLocalFS(CSV_FILE_PATH);
        AnalysisJob analysisJob = buildAnalysisJob(analyzerBeansConfiguration, CSV_FILE_PATH);
        String analyzerBeansConfigurationDatastores = ConfigurationSerializer
                .serializeAnalyzerBeansConfigurationDataStores(analyzerBeansConfiguration);
        String analysisJobXml = ConfigurationSerializer.serializeAnalysisJobToXml(analyzerBeansConfiguration,
                analysisJob);
        FlatFileMapper flatFileMapper = new FlatFileMapper();
        FlatFileReducer flatFileReducer = new FlatFileReducer();
        mapDriver = MapDriver.newMapDriver(flatFileMapper);
        mapDriver.getConfiguration().set(FlatFileTool.ANALYZER_BEANS_CONFIGURATION_DATASTORES_KEY,
                analyzerBeansConfigurationDatastores);
        mapDriver.getConfiguration().set(FlatFileTool.ANALYSIS_JOB_XML_KEY, analysisJobXml);
        reduceDriver = ReduceDriver.newReduceDriver(flatFileReducer);
        reduceDriver.getConfiguration().set(FlatFileTool.ANALYZER_BEANS_CONFIGURATION_DATASTORES_KEY,
                analyzerBeansConfigurationDatastores);
        reduceDriver.getConfiguration().set(FlatFileTool.ANALYSIS_JOB_XML_KEY, analysisJobXml);
        mapReduceDriver = MapReduceDriver.newMapReduceDriver(flatFileMapper, flatFileReducer);
    }

    @Test
    public void testMapper() {
        mapDriver.withInput(new LongWritable(0), new Text(
                "Country name;ISO 3166-2;ISO 3166-3;ISO Numeric;Linked to country;Synonym1;Synonym2;Synonym3"));
        mapDriver.withInput(new LongWritable(44), new Text("Poland;PL;POL;616;"));
        mapDriver.withInput(new LongWritable(66), new Text("Denmark;DK;DNK;208;;Danmark;Danemark;"));
        mapDriver.runTest();
    }

    @Test
    public void testReducerHeader() {
        List<SortedMapWritable> rows = new ArrayList<SortedMapWritable>();

        SortedMapWritable header = new SortedMapWritable();
        header.put(new Text("ISO 3166-2_ISO 3166-3"), new Text("ISO 3166-2_ISO 3166-3"));
        header.put(new Text("Country name"), new Text("Country name"));
        header.put(new Text("ISO 3166-2"), new Text("ISO 3166-2"));
        header.put(new Text("ISO 3166-3"), new Text("ISO 3166-3"));
        header.put(new Text("ISO Numeric"), new Text("ISO Numeric"));
        header.put(new Text("Linked to country"), new Text("Linked to country"));
        header.put(new Text("Synonym1"), new Text("Synonym1"));
        header.put(new Text("Synonym2"), new Text("Synonym2"));
        header.put(new Text("Synonym3"), new Text("Synonym3"));
        rows.add(header);

        reduceDriver.withInput(new LongWritable(0), rows);
        reduceDriver
                .withOutput(
                        new LongWritable(0),
                        new Text(
                                "Country name;ISO 3166-2;ISO 3166-2_ISO 3166-3;ISO 3166-3;ISO Numeric;Linked to country;Synonym1;Synonym2;Synonym3\n"));
        reduceDriver.runTest();
    }

    @Test
    public void testReducerPoland() {
        List<SortedMapWritable> rows = new ArrayList<SortedMapWritable>();

        SortedMapWritable poland = new SortedMapWritable();
        poland.put(new Text("Country name"), new Text("Poland"));
        poland.put(new Text("ISO 3166-2"), new Text("PL"));
        poland.put(new Text("ISO 3166-3"), new Text("POL"));
        rows.add(poland);

        reduceDriver.withInput(new LongWritable(44), rows);
        reduceDriver.withOutput(new LongWritable(44), new Text("Poland;PL;POL\n"));
        reduceDriver.runTest();

    }

    public static AnalyzerBeansConfiguration buildAnalyzerBeansConfigurationLocalFS(String csvFilePath) {
        CsvConfiguration csvConfiguration = new CsvConfiguration(1, "UTF8", ';', '"', '\\');
        Datastore datastore = new CsvDatastore(csvFilePath, new FileResource(csvFilePath), csvConfiguration);

        DatastoreCatalog datastoreCatalog = new DatastoreCatalogImpl(datastore);

        SimpleDescriptorProvider descriptorProvider = new SimpleDescriptorProvider(true);
        descriptorProvider.addTransformerBeanDescriptor(Descriptors.ofTransformer(ConcatenatorTransformer.class));
        descriptorProvider.addTransformerBeanDescriptor(Descriptors.ofTransformer(TokenizerTransformer.class));
        descriptorProvider.addAnalyzerBeanDescriptor(Descriptors.ofAnalyzer(InsertIntoTableAnalyzer.class));
        descriptorProvider.addAnalyzerBeanDescriptor(Descriptors.ofAnalyzer(StringAnalyzer.class));

        return new AnalyzerBeansConfigurationImpl().replace(datastoreCatalog).replace(descriptorProvider);
    }

    public static AnalysisJob buildAnalysisJob(AnalyzerBeansConfiguration configuration, String datastoreName) {
        AnalysisJobBuilder ajb = new AnalysisJobBuilder(configuration);
        try {
            ajb.setDatastore(datastoreName);
            ajb.addSourceColumns("countrycodes.csv.countrycodes.Country name",
                    "countrycodes.csv.countrycodes.ISO 3166-2", "countrycodes.csv.countrycodes.ISO 3166-3",
                    "countrycodes.csv.countrycodes.Synonym3");

            TransformerJobBuilder<ConcatenatorTransformer> concatenator = ajb
                    .addTransformer(ConcatenatorTransformer.class);
            concatenator.addInputColumns(ajb.getSourceColumnByName("countrycodes.csv.countrycodes.ISO 3166-2"));
            concatenator.addInputColumns(ajb.getSourceColumnByName("countrycodes.csv.countrycodes.ISO 3166-3"));
            concatenator.setConfiguredProperty("Separator", "_");

            AnalyzerJobBuilder<ValueDistributionAnalyzer> valueDistributionAnalyzer = ajb
                    .addAnalyzer(ValueDistributionAnalyzer.class);
            valueDistributionAnalyzer.addInputColumn(ajb
                    .getSourceColumnByName("countrycodes.csv.countrycodes.Country name"));

            AnalyzerJobBuilder<ValueDistributionAnalyzer> valueDistributionAnalyzer2 = ajb
                    .addAnalyzer(ValueDistributionAnalyzer.class);
            valueDistributionAnalyzer2.addInputColumn(ajb
                    .getSourceColumnByName("countrycodes.csv.countrycodes.ISO 3166-2"));

            return ajb.toAnalysisJob();
        } finally {
            ajb.close();
        }
    }

}