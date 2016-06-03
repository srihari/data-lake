package datalake.ri.datapipeline.workflow.actions;

import org.apache.prepbuddy.datacleansers.RowPurger;
import org.apache.prepbuddy.normalizers.ZScoreNormalization;
import org.apache.prepbuddy.rdds.TransformableRDD;
import org.apache.prepbuddy.typesystem.FileType;
import org.apache.prepbuddy.utils.RowRecord;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;

public class FlightDelayRecordNormalizer extends FlightDelayPipeline{
    public static void main(String[] args) {
        initialize(args);
        TransformableRDD travelledFlights = new TransformableRDD(javaSparkContext.textFile("s3://twi-analytics-sandbox/dev-workspaces/" + user + "/data/input/" + inputFileName));

        TransformableRDD normalizedData = travelledFlights
                .deduplicate()
                .removeRows(new RowPurger.Predicate() {
                    @Override
                    public Boolean evaluate(RowRecord rowRecord) {
                        boolean hasNA = false;
                        for (int index = 0; index < 6; index++)
                            hasNA = hasNA || rowRecord.valueAt(index).trim().equals("NA");
                        return hasNA;
                    }
                })
                .normalize(0, new ZScoreNormalization())
                .normalize(1, new ZScoreNormalization())
                .normalize(2, new ZScoreNormalization())
                .normalize(3, new ZScoreNormalization())
                .normalize(4, new ZScoreNormalization())
                .map(new Function<String, String>() {
                    @Override
                    public String call(String record) throws Exception {
                        String[] recordAsArray = FileType.CSV.parseRecord(record);
                        double arrivalDelay = Double.parseDouble(recordAsArray[5]);
                        String label = arrivalDelay >= 10 ? "1" : "0";
                        recordAsArray[5] = label;
                        return FileType.CSV.join(recordAsArray);
                    }
                });

        normalizedData.saveAsTextFile("s3://twi-analytics-sandbox/dev-workspaces/" + user + "/data/normalized/" + outputFileName);
    }
}