package data.job;

public enum ValidationBatchBatchJobSubType implements BatchJobSubType {
  EXTERNAL, INTERNAL;

  @Override
  public String getName() {
    return name();
  }
}
