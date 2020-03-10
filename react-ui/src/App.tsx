import React from 'react';
import { Switch, Route, Redirect } from "react-router-dom";
import ClusterList from './components/clusterlist/ClusterList';
import { NavBar } from './components/navbar/NavBar';
import './App.css';
import ClusterDetails from './components/clusterdetails/ClusterDetails';
import Cluster from './interfaces/Cluster'
import InstancePage from './components/instancepage/InstancePage';
import CreateCluster from './components/createcluster/CreateCluster';
import CreateInstance from './components/createinstance/CreateInstance';

interface AppState {
  clusters: Array<Cluster>;
}

export class App extends React.Component<any, AppState> {
  constructor(props: any) {
    super(props);
    this.state = {
      clusters: Array<Cluster>()
    };
  }

  handleUpdateClusters = (new_clusters: Array<Cluster>): void => {
    this.setState({
      clusters: new_clusters
    });
  }

  render() {
    return (
      <div className="App">
        <NavBar />
        <Switch>
          <Route path="/clusters/new">
            <CreateCluster />
          </Route>
          <Route path="/clusters/:id">
            <ClusterDetails />
          </Route>
          <Route path="/clusters">
            <ClusterList clusters={this.state.clusters} handleUpdateClusters={this.handleUpdateClusters} />
          </Route>
          <Route path="/instances/new">
            <CreateInstance />
          </Route>
          <Route path="/instances">
            <InstancePage />
          </Route>
          {/* Redirect to cluster list by default */}
          <Route path="/">
            <Redirect to="/clusters" />
          </Route>
        </Switch>
        
      </div>
    );
  }
}
