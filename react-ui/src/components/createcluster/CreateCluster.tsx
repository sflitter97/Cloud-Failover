import React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import styles from './CreateCluster.module.css';
import ClusterUpdateDto from '../../interfaces/ClusterUpdateDto';
import { API_METHODS as API } from '../../api_methods';
import ClusterForm from '../clusterform/ClusterForm';

export interface CreateClusterProps extends RouteComponentProps<{id: string}> {
}

interface CreateClusterState {
}

class CreateCluster extends React.Component<CreateClusterProps, CreateClusterState> {
  onSubmit = (formData: ClusterUpdateDto, accessInstance: {} | undefined) => {
    fetch(API.BASE + API.CLUSTERS, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(formData)
    })
    .then(res => res.json())
    .then(cluster => {
      if(accessInstance !== undefined) {
        fetch(cluster._links.traffic.href, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(accessInstance)
        })
        .then(res => res.json())
        .catch(console.log)
      }
    })
    .catch(console.log)
    .then(_ => this.props.history.push("/clusters"))
  }

  render() {
    return (
      <div className={styles.createCluster}>
        <ClusterForm 
          submitButtonName="Create"
          onSubmit={this.onSubmit}
        />
      </div>
    );
  }
}

export default withRouter(CreateCluster)