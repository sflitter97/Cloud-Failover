import React from 'react';
import styles from './InstancePage.module.css';
import Instance from '../../interfaces/Instance';
import { InstanceList } from '../instancelist/InstanceList';
import Row from 'react-bootstrap/Row';
import Button from 'react-bootstrap/Button';
import { RouteComponentProps, withRouter, Link } from 'react-router-dom';
var deepEqual = require('fast-deep-equal/es6/react');

export interface InstancePageProps extends RouteComponentProps {
}

interface InstancePageState {
  selected: Array<{}>;
  instances: Array<Instance>;
}

class InstancePage extends React.Component<InstancePageProps, InstancePageState> {
  constructor(props: InstancePageProps) {
    super(props);
    this.state = {
      selected: new Array<{}>(),
      instances: new Array<Instance>()
    }
  }

  handleSelectInstance = (row: any, isSelect: boolean) => {
    console.log(this.state.selected)
    if(isSelect) {
      this.setState({
        selected: this.state.selected.concat([row.instance.handle as {}])
      })
    } else {
      this.setState({
        selected: this.state.selected.filter(value => !deepEqual(value, row.instance.handle))
      });
    }
  }

  updateInstanceInfos = (instanceInfos: Array<Instance>) => {
    this.setState({
      instances: instanceInfos
    })
  }

  getSelectedInstance = () => {
    return this.state.selected.map(handle => {
      return this.state.instances.find(instance => {
        return deepEqual(handle, instance.handle);
      });
    });
  }

  handleStart = () => {
    const selectedInstances = this.getSelectedInstance();

    const responses = selectedInstances.map(instance => {
      return fetch(instance?._links.start.href || "", {
        method: 'PUT'
      })
    })

    Promise.all<Response>(responses)
    .then(data => console.log(data));
  }

  handleStop = () => {
    const selectedInstances = this.getSelectedInstance();

    const responses = selectedInstances.map(instance => {
      return fetch(instance?._links.stop.href || "", {
        method: 'PUT'
      })
    })

    Promise.all<Response>(responses)
    .then(data => console.log(data));
  }

  handleDelete = () => {
    const selectedInstances = this.getSelectedInstance();

    const responses = selectedInstances.map(instance => {
      return fetch(instance?._links.delete.href || "", {
        method: 'DELETE'
      })
    })

    Promise.all<Response>(responses)
    .then(data => console.log(data));
  }

  render() {
    return (
      <div className={styles.instancePage}>
        <Row className={styles.right}>
          <Link to="/instances/new"><Button className={styles.button} variant="primary">Create</Button></Link>
          <Button className={styles.button} variant="primary" onClick={this.handleStart}>Start</Button>
          <Button className={styles.button} variant="warning" onClick={this.handleStop}>Stop</Button>
          <Button className={styles.button} variant="danger" onClick={this.handleDelete}>Delete</Button>
        </Row>
        <InstanceList
          onRowClick={this.handleSelectInstance}
          updateInstanceInfos={this.updateInstanceInfos}/>
      </div>
    );
  }
}

export default withRouter(InstancePage)