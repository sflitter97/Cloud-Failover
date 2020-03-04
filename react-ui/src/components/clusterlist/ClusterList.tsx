import React from 'react';
import Table from 'react-bootstrap/Table';
import styles from './ClusterList.module.css';
import Cluster from '../../interfaces/Cluster';
import ClusterUpdateDto from '../../interfaces/ClusterUpdateDto';
import { ClusterModal } from '../clustermodal/ClusterModal';
import { API_METHODS as API } from '../../api_methods';
import 'react-bootstrap-table-next/dist/react-bootstrap-table2.min.css';
import BootstrapTable from 'react-bootstrap-table-next';

export interface ClusterListProps {
}

interface ClusterListState {
  clusters: Array<Cluster>;
  selectedCluster: number;
  showModal: boolean;
}

export class ClusterList extends React.Component<ClusterListProps, ClusterListState> {
  constructor(props: ClusterListProps) {
    super(props);
    this.state = {
      clusters: Array<Cluster>(),
      selectedCluster: 0,
      showModal: false

    };
  }

  componentDidMount() {
    // fetch cluster list
    fetch(API.BASE + API.CLUSTERS)
    .then(res => res.json())
    // fetch details of each cluster individually
    .then(data => {
      return data._embedded.clusters.map((cluster: { _links: { self: { href: RequestInfo; }; }; }) =>
				fetch(cluster._links.self.href)
		  );
    })
    // gather the individual response promises into one
    .then(clusterPromises => Promise.all<Response>(clusterPromises))
    // convert each to json
    .then(clusterPromises => {
      return clusterPromises.map((value) => value.json())
    })
    // gather the individual json promises into one
    .then(clusterJsonPromises => Promise.all<Cluster>(clusterJsonPromises))
    .then(clusterJson => {
      this.setState({
        clusters: clusterJson,
        selectedCluster: 0,
        showModal: false
      })
    })
    .catch(console.log);
  }

  handleRowClick(idx: number) {
    console.log(idx);
    if(idx >= 0 && idx < this.state.clusters.length) {
      this.setState({
        selectedCluster: idx,
        showModal: true
      });
    }
  }

  handleModalClose = () => {
    this.setState({
      showModal: false
    });
  }

  handleModalFormSubmit = (cluster: ClusterUpdateDto) => {
    console.log(JSON.stringify(cluster))
    fetch(API.BASE + API.CLUSTERS + "/" + this.state.clusters[this.state.selectedCluster].id, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(cluster)
    })
      .then(res => res.json())
      .then(res => console.log(res))
    this.setState(state => {
      const clusters = state.clusters.map((value, index) => {
        if(index === state.selectedCluster) {
          let new_cluster = value;
          if(cluster.name !== undefined) {
            value.name = cluster.name;
          }
          if(cluster.instances !== undefined) {
            value.instances = cluster.instances;
          }
          if(cluster.targetPort !== undefined) {
            value.targetPort = cluster.targetPort;
          }
          if(cluster.targetPath !== undefined) {
            value.targetPath = cluster.targetPath;
          }
          return new_cluster;
        } else {
          return value;
        }
      });

      return {clusters};
    });
    console.log(cluster);
  }

  render() {
    const columns = [
      {
        dataField: 'id',
        hidden: true
      },
      {
        dataField: 'name',
        text: 'Name',
        sort: true
      },
      {
        dataField: 'targetPort',
        text: 'Target Port',
        type: "number"
      },
      {
        dataField: 'targetPath',
        text: 'Target Path'
      },
      {
        dataField: 'instances',
        text: 'Instances',
        type: "number"
      },
      {
        dataField: 'access',
        text: 'Access',
        formatter: (cell: string, row: any, rowIndex: number, formatExtraData: any) => {
          return <a href={cell} rel="noopener noreferrer" target="_blank">{cell}</a>;
        }
      }
    ]
    const selectRow = {
      mode: 'checkbox',
      onSelect: (row: any, isSelect: any, rowIndex: any, e: any) => {
        if(e.target.tagName === 'A') {
          return false;
        }
        console.log(row)
      },
      clickToSelect: true,
      hideSelectColumn: true
    };
    return (
      <div className={styles.clusterList}>
          <h2>Clusters</h2>
          <BootstrapTable 
            keyField='name'
            data={this.state.clusters.map(cluster => {
              console.log(cluster);
              return {
                id: cluster.id,
                name: cluster.name,
                targetPort: cluster.targetPort,
                targetPath: cluster.targetPath,
                instances: cluster.instances.length,
                access: cluster._links.access.href
              }
            })} 
            columns={columns}
            bootstrap4={true}
            bordered={false}
            hover={true}
            selectRow={selectRow}/>
        {this.state.showModal && <ClusterModal
          cluster={this.state.clusters[this.state.selectedCluster]}
          accessApi={API.BASE + API.ACCESS}
          show={true}
          handleClose={this.handleModalClose}
          handleFormSubmit={this.handleModalFormSubmit} />
        }
      </div>
    );
  }
}
