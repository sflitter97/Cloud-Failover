import React from 'react';
import Table from 'react-bootstrap/Table';
import styles from './ClusterList.module.css';
import Cluster from '../../interfaces/Cluster';
import ClusterUpdateDto from '../../interfaces/ClusterUpdateDto';
import { ClusterModal } from '../clustermodal/ClusterModal';
import { API_METHODS as API } from '../../api_methods';

export interface ClusterListProps {
}

interface ClusterListState {
  clusters: Array<Cluster>;
  selectedCluster: number;
  formCluster: Cluster;
  showModal: boolean;
}

export class ClusterList extends React.Component<ClusterListProps, ClusterListState> {
  componentDidMount() {
    fetch(API.BASE + API.CLUSTERS)
    .then(res => res.json())
    .then(data => {
      return data._embedded.clusters.map((cluster: { _links: { self: { href: RequestInfo; }; }; }) =>
				fetch(cluster._links.self.href)
		  );
    })
    .then(clusterPromises => {
      return Promise.all<Response>(clusterPromises)
    })
    .then(clusterPromises => {
      return clusterPromises.map<Cluster>((value: any) => {
        console.log(value)
        return value.res()
      });
    })
    // .then(data => console.log(data));
    // .then(data => {
    //   this.setState({
    //     clusters: data,
    //     selectedCluster: 0,
    //     formCluster: data[0],
    //     showModal: false
    //   })
    //   console.log(data)
    // })
    // .catch(console.log);
  }

  handleRowClick(idx: number) {
    console.log(idx);
    if(idx >= 0 && idx < this.state.clusters.length) {
      this.setState({
        selectedCluster: idx,
        formCluster: this.state.clusters[idx],
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
    return (
      <div className={styles.clusterList}>
          <h2>Clusters</h2>
          <Table striped hover className={styles.table}>
            <thead>
              <tr>
                <th>UUID</th>
                <th>Name</th>
                <th>Port</th>
                <th>Path</th>
                <th>Instances</th>
                <th>Access URL</th>
              </tr>
            </thead>
            <tbody>
              {this.state !== null && this.state.clusters.map((value, index) => {
                return <tr key={value.id} onClick={() => this.handleRowClick(index)}>
                  <td>{value.id}</td>
                  <td>{value.name}</td>
                  <td>{value.targetPort}</td>
                  <td>{value.targetPath}</td>
                  <td>{value.instances.length}</td>
                  <td><a href={API.BASE + API.ACCESS + "/" + value.id + "/"} rel="noopener noreferrer" target="_blank">
                    {API.BASE + API.ACCESS + "/" + value.id + "/"}
                  </a></td>
                </tr>
              })}
            </tbody>
        </Table>
        {this.state !== null && this.state.showModal && <ClusterModal
          cluster={this.state.formCluster}
          accessApi={API.BASE + API.ACCESS}
          show={true}
          handleClose={this.handleModalClose}
          handleFormSubmit={this.handleModalFormSubmit} />
        }
      </div>
    );
  }
}
