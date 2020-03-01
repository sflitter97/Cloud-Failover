import React from 'react';
import { ClusterList } from './components/clusterlist/ClusterList'
import './App.css';

export class App extends React.Component {
  render() {
    return (
      <div className="App">
        <header className="App-header">
          Cloud Failover
        </header>
        <ClusterList />
      </div>
    );
  }
}
