import React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import styles from './ClusterDetails.module.css';
import Cluster from '../../interfaces/Cluster';
import ClusterUpdateDto from '../../interfaces/ClusterUpdateDto';
import { API_METHODS as API } from '../../api_methods';
import ClusterForm from '../clusterform/ClusterForm';
import { Loading } from '../loading/Loading';
import Button from 'react-bootstrap/Button';
var deepEqual = require('fast-deep-equal/es6/react');

export interface ClusterDetailsProps extends RouteComponentProps<{id: string}> {
}

interface ClusterDetailsState {
  selectedCluster: Cluster;
}

class ClusterDetails extends React.Component<ClusterDetailsProps, ClusterDetailsState> {

  componentDidMount() {
    fetch(API.BASE + API.CLUSTERS)
    .then(res => res.json())
    // fetch details of each cluster individually
    .then(data => {
      return data._embedded.clusters.find((cluster: Cluster) =>
				cluster.id === this.props.match.params.id
		  );
    })
    .then((cluster: Cluster) => {
      return fetch(cluster._links.self.href)
    })
    .then(res => res.json())
    .then(data => {
      this.setState({
        selectedCluster: data
      })
    })
  }

  onSubmit = (formData: ClusterUpdateDto, accessInstance: {} | undefined) => {
    let postData: ClusterUpdateDto = {}
    if(formData.name !== undefined && formData.name !== this.state.selectedCluster.name) {
      postData.name = formData.name
    }
    console.log(formData.instances)
    console.log(this.state.selectedCluster.instances)
    console.log(deepEqual(formData.instances), this.state.selectedCluster.instances)
    if(formData.instances !== undefined && !deepEqual(formData.instances, this.state.selectedCluster.instances)) {
      postData.instances = formData.instances
    }
    if(formData.targetPort !== undefined && formData.targetPort !== this.state.selectedCluster.targetPort) {
      postData.targetPort = formData.targetPort
    }
    if(formData.targetPath !== undefined && formData.targetPath !== this.state.selectedCluster.targetPath) {
      postData.targetPath = formData.targetPath
    }
    console.log(postData)
    let res = fetch(this.state.selectedCluster._links.self.href, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(postData)
    })
    .then(res => res.json())
    .catch(console.log)

    let res2 = Promise.resolve()
    if(accessInstance !== undefined) {
      res2 = fetch(this.state.selectedCluster._links.traffic.href, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(accessInstance)
      })
      .then(res => res.json())
      .catch(console.log)
    }

    Promise.all([res, res2])
    .then(_ => this.props.history.push("/clusters"))
  }

  handleDelete = () => {
    fetch(this.state.selectedCluster._links.self.href, {
      method: 'DELETE'
    })
    .then(_ => this.props.history.push("/clusters"))
  }

  render() {
    return (
      <div className={styles.clusterDetails}>
        { this.state !== null ?
          <div>
            <ClusterForm 
              cluster={this.state.selectedCluster}
              submitButtonName="Save"
              onSubmit={this.onSubmit}
            />
            <Button className={styles.button} variant="danger" onClick={this.handleDelete}>
            Delete
            </Button>
          </div>
        : <Loading iconSize="3x" fontSize="2rem" />}
      </div>
    );
  }
}

export default withRouter(ClusterDetails)