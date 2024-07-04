package data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DbCleaner {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String JUNIT_DATASET= "JUnit";

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionTemplate transactionTemplate;

  private static final String[] TABLES = {
      "Execution_Record",
      "Execution_Record_Exception_Log",
      "Execution_Record_External_Identifier",
      "Task_Info",
  };

  private static final String[] SELECTIVE_DELETE_REQUESTS = {
      "DELETE from \"batch-framework\".Execution_Record WHERE dataset_Id <> '"+JUNIT_DATASET+"' OR execution_Id NOT IN :set",
      "DELETE from \"batch-framework\".Execution_Record_Exception_Log WHERE dataset_Id <> '"+JUNIT_DATASET+"' OR execution_Id NOT IN :set",
      "DELETE from \"batch-framework\".Execution_Record_External_Identifier WHERE dataset_Id <> '"+JUNIT_DATASET+"' OR execution_Id NOT IN :set",
      "DELETE from \"batch-framework\".Task_Info WHERE CAST(task_id as VARCHAR) NOT IN :set",
  };


  public void clearDbFor(int stepNumber) {
    LOGGER.info("Clearing data for test for the workflow step number: {}", stepNumber);
    for (int tableNumber = 0; tableNumber < TABLES.length; tableNumber++) {
      clearTable(stepNumber, tableNumber);
    }
  }

  private void clearTable(int stepNumber, int tableNumber) {
    int result = transactionTemplate.execute(status -> {
      Query query;
      if (stepNumber == 1) {
        query = createTruncateWholeTableQuery(tableNumber);
      } else {
        query = createDeleteAllButNeededDataQuery(stepNumber, tableNumber);
      }
      return query.executeUpdate();
    });
    LOGGER.info("Cleared data in the table: {}, removed {} rows.", TABLES[tableNumber], result);
  }

  private Query createDeleteAllButNeededDataQuery(int stepNumber, int tableNumber) {
    Query query;
    Set<String> set = IntStream.range(1, stepNumber).mapToObj(String::valueOf).collect(Collectors.toUnmodifiableSet());

    query = entityManager
        .createNativeQuery(SELECTIVE_DELETE_REQUESTS[tableNumber])
        .setParameter("set", set);
    return query;
  }

  private Query createTruncateWholeTableQuery(int tableNumber) {
    Query query;
    query = entityManager.createNativeQuery("TRUNCATE TABLE \"batch-framework\"." + TABLES[tableNumber]);
    return query;
  }
}
