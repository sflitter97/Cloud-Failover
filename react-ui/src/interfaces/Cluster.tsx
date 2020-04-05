import ClusterState from './ClusterState'
export type Cluster = {
  _links: { [key: string]: {'href': string}}
  id: string;
  name: string;
  instances: Array<{}>;
  targetPort: number;
  targetPath: string;
  accessInstance: {};
  backupInstance: {};
  enableInstanceStateManagement: string;
  enableHotBackup: string;
  enableAutomaticPriorityAdjustment: string;
  state: ClusterState;
}

export default Cluster