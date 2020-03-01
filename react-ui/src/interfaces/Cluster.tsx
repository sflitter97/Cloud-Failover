export type Cluster = {
  id: string;
  name: string;
  instances: Array<any>;
  targetPort: number;
  targetPath: string;
}

export default Cluster