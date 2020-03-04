import React from 'react';
import { Switch, Route, Redirect } from "react-router-dom";
import { ClusterList } from './components/clusterlist/ClusterList';
import { InstanceList } from './components/instancelist/InstanceList';
import { NavBar } from './components/navbar/NavBar';
import './App.css';

export class App extends React.Component {
  render() {
    return (
      <div className="App">
        <NavBar />
        <Switch>
          <Route path="/clusters">
            <ClusterList />
          </Route>
          <Route path="/instances">
            <InstanceList />
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
