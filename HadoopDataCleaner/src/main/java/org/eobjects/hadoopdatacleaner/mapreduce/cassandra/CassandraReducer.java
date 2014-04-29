/**
 * DataCleaner (community edition)
 * Copyright (C) 2013 Human Inference

 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.hadoopdatacleaner.mapreduce.cassandra;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SortedMapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Reducer;
import org.eobjects.analyzer.beans.api.Analyzer;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.result.AnalyzerResult;
import org.eobjects.hadoopdatacleaner.configuration.ConfigurationSerializer;
import org.eobjects.hadoopdatacleaner.datastores.RowUtils;
import org.eobjects.hadoopdatacleaner.tools.FlatFileTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraReducer extends
Reducer</* KEYIN */Text, /* VALUEIN */SortedMapWritable, /*KEYOUT*/NullWritable, /*VALUEOUT*/List<ByteBuffer>> {

    private static final Logger logger = LoggerFactory.getLogger(CassandraReducer.class);

    private AnalyzerBeansConfiguration analyzerBeansConfiguration;

    private AnalysisJob analysisJob;

    @Override
    protected void setup(
            org.apache.hadoop.mapreduce.Reducer</* KEYIN */Text, /* VALUEIN */SortedMapWritable, /*KEYOUT*/NullWritable, /*VALUEOUT*/List<ByteBuffer>>.Context context)
            throws IOException, InterruptedException {
        Configuration mapReduceConfiguration = context.getConfiguration();
        String datastoresConfigurationLines = mapReduceConfiguration
                .get(FlatFileTool.ANALYZER_BEANS_CONFIGURATION_DATASTORES_KEY);
        String analysisJobXml = mapReduceConfiguration.get(FlatFileTool.ANALYSIS_JOB_XML_KEY);
        analyzerBeansConfiguration = ConfigurationSerializer
                .deserializeAnalyzerBeansDatastores(datastoresConfigurationLines);
        analysisJob = ConfigurationSerializer.deserializeAnalysisJobFromXml(analysisJobXml, analyzerBeansConfiguration);
        super.setup(context);
    }

    public void reduce(Text analyzerKey, Iterable<SortedMapWritable> writableResults, Context context)
            throws IOException, InterruptedException {

        System.out.println("Reducer syso statement!");
        
//        Analyzer<?> analyzer = ConfigurationSerializer.initializeAnalyzer(analyzerKey.toString(), analyzerBeansConfiguration, analysisJob);
//
//        logger.info("analyzerKey = " + analyzerKey.toString() + " rows: ");
//        for (SortedMapWritable rowWritable : writableResults) {
//            InputRow inputRow = RowUtils.sortedMapWritableToInputRow(rowWritable, analysisJob.getSourceColumns());
//            analyzer.run(inputRow, 1);
//
//            List<ByteBuffer> cassandraRecord = new ArrayList<ByteBuffer>();
//            for (@SuppressWarnings("rawtypes") Map.Entry<WritableComparable, Writable> rowEntry : rowWritable.entrySet()) {
////                Text columnName = (Text) rowEntry.getKey();
//                Text columnValue = (Text) rowEntry.getValue();
//                    
//                cassandraRecord.add(ByteBufferUtil.bytes(columnValue.toString()));         
//            }
//            
//            context.write(NullWritable.get(), cassandraRecord);
//        }
//        logger.info("end of analyzerKey = " + analyzerKey.toString() + " rows.");
//
//        AnalyzerResult analyzerResult = analyzer.getResult();
//        logger.debug("analyzerResult.toString(): " + analyzerResult.toString());
    }
    
}
