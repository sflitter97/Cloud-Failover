class ClusterState {
  public static No_Instances = new ClusterState("No Instances");
  public static Operational = new ClusterState("Operational");
  public static Transitioning = new ClusterState("Transitioning");
  public static Failed = new ClusterState("Failed");
  public static Unknown = new ClusterState("Unknown");

  private static readonly stringLookup = new Map<String, ClusterState>([
    ["no_instances", ClusterState.No_Instances],
    ["operational", ClusterState.Operational],
    ["transitioning", ClusterState.Transitioning],
    ["failed", ClusterState.Failed],
    ["unknown", ClusterState.Unknown]
  ]);

  private constructor(private state: string) {}

  public static getState(state: String): ClusterState {
    state = state.toLowerCase()
    const clusterState = ClusterState.stringLookup.get(state);
    if(clusterState === undefined)
      return ClusterState.Unknown;
    return clusterState;
  }

  public toString(): string {
    return this.state;
  }
}
  
export default ClusterState