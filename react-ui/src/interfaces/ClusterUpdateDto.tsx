export type ClusterUpdateDto = {
    name?: string;
    instances?: Array<any>;
    targetPort?: number;
    targetPath?: string;
    accessInstance?: {};
    backupInstance?: {};
    enableInstanceStateManagement?: string;
    enableHotBackup?: string;
    enableAutomaticPriorityAdjustment?: string;
  }
  
  export default ClusterUpdateDto