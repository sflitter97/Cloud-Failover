export type ClusterUpdateDto = {
    name?: string;
    instances?: Array<any>;
    targetPort?: number;
    targetPath?: string;
  }
  
  export default ClusterUpdateDto