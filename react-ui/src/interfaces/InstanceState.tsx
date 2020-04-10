class InstanceState {
  public static Stopped = new InstanceState("Stopped");
  public static Running = new InstanceState("Running");
  public static Pending = new InstanceState("Pending");
  public static Stopping = new InstanceState("Stopping");
  public static Deleted =  new InstanceState("Deleted");
  public static Deleting = new InstanceState("Deleting");
  public static Unknown = new InstanceState("Unknown");
  public static Provisioning = new InstanceState("Provisioning");
  public static Staging = new InstanceState("Staging");
  public static Repairing = new InstanceState("Repairing");
  public static Terminated = new InstanceState("Terminated");
  public static Deallocated = new InstanceState("Deallocated");
  public static Deallocating = new InstanceState("Deallocating");
  public static Starting = new InstanceState("Starting");

  private static readonly stringLookup = new Map<String, InstanceState>([
    ["stopped", InstanceState.Stopped],
    ["running", InstanceState.Running],
    ["pending", InstanceState.Pending],
    ["stopping", InstanceState.Stopping],
    ["deleted", InstanceState.Deleted],
    ["deleting", InstanceState.Deleting],
    ["unknown", InstanceState.Unknown],
    ["provisioning", InstanceState.Provisioning],
    ["staging", InstanceState.Staging],
    ["repairing", InstanceState.Repairing],
    ["terminated", InstanceState.Terminated],
    ["deallocated", InstanceState.Deallocated],
    ["deallocating", InstanceState.Deallocating],
    ["starting", InstanceState.Starting]
  ]);

  private constructor(private state: string) {}

  public static getState(state: String): InstanceState {
    state = state.toLowerCase()
    const instanceState = InstanceState.stringLookup.get(state);
    if(instanceState === undefined)
      return InstanceState.Unknown;
    return instanceState;
  }

  public toString(): string {
    return this.state;
  }
}
  
export default InstanceState