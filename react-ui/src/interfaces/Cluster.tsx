export type Cluster = {
  _links: { [key: string]: {'href': string}}
  id: string;
  name: string;
  instances: Array<any>;
  targetPort: number;
  targetPath: string;
}

export default Cluster