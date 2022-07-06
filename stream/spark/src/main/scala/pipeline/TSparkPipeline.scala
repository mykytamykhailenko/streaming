package pipeline

import com.google.inject.ImplementedBy
import org.apache.spark.sql.DataFrame

@ImplementedBy(classOf[SparkPipeline])
trait TSparkPipeline {

  def build(source: DataFrame): DataFrame

}
