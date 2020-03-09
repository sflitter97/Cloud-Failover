export type Cluster = {
  _links: { [key: string]: {'href': string}}
  id: string;
  name: string;
  instances: Array<{}>;
  targetPort: number;
  targetPath: string;
  accessInstance: {};
}

export default Cluster