import React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom'
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import styles from './ClusterForm.module.css';
import Cluster from '../../interfaces/Cluster';
import ClusterUpdateDto from '../../interfaces/ClusterUpdateDto';
import Instance from '../../interfaces/Instance';
import { InstanceList } from '../instancelist/InstanceList';
var deepEqual = require('fast-deep-equal/es6/react');

export interface ClusterFormProps extends RouteComponentProps<{id: string}> {
  cluster?: Cluster;
  submitButtonName: string;
  onSubmit: (formData: ClusterUpdateDto, accessInstance: {} | undefined) => void
}

interface ClusterFormState {
  formControls: {
    name: string,
    instances: Array<{}>,
    targetPort: number,
    targetPath: string,
    accessInstance?: {}
  };
  instanceInfos: Array<Instance>;
}

class ClusterForm extends React.Component<ClusterFormProps, ClusterFormState> {
  constructor(props: ClusterFormProps) {
    super(props);
    this.state = {
      formControls: {
        name: props.cluster?.name || "",
        instances: props.cluster?.instances || Array<{}>(),
        targetPort: props.cluster?.targetPort || 0,
        targetPath: props.cluster?.targetPath || "",
        accessInstance: props.cluster?.accessInstance
      },
      instanceInfos: Array<Instance>()
    }
  }

  handleChange = (e: React.FormEvent<HTMLInputElement>) => {
    // e.persist()
    // console.log(e.currentTarget);
    let value = e.currentTarget.value
    if(e.currentTarget.name === 'accessInstance') {
      value = JSON.parse(value);
    }
    this.setState({
      formControls: {
        ...this.state.formControls,
        [e.currentTarget.name]: value
      }
    });
  }

  handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const formData: ClusterUpdateDto = {
      name: this.state.formControls.name,
      instances: this.state.formControls.instances,
      targetPort: this.state.formControls.targetPort,
      targetPath: this.state.formControls.targetPath
    }
    this.props.onSubmit(formData, this.state.formControls.accessInstance)
  }

  handleCancel = () => {
    this.props.history.goBack();
  }

  handleSelectInstance = (row: any, isSelect: boolean) => {
    console.log(this.state.formControls.instances)
    if(isSelect) {
      this.setState({
        formControls: {
          ...this.state.formControls,
          instances: this.state.formControls.instances.concat([row.instance.handle as {}])
        }
      })
    } else {
      this.setState({
        formControls: {
          ...this.state.formControls,
          instances: this.state.formControls.instances.filter(value => !deepEqual(value, row.instance.handle))
        }
      });
    }
  }

  updateInstanceInfos = (instanceInfos: Array<Instance>) => {
    this.setState({
      instanceInfos: instanceInfos
    })
  }

  generateSelectOptions() {
    let options = this.state.formControls.instances.map(instance => {
      const info = this.state.instanceInfos.find(value => deepEqual(value.handle, instance))
      return <option key={JSON.stringify(instance)} value={JSON.stringify(instance)}>{`Provider: ${info?.provider || ""} Name: ${info?.name || ""}`}</option>
    })
    options.unshift(<option key={"please-select"} value={"please-select"} disabled={true}>Please Select</option>)

    return options
  }

  render() {
    return (
      <div className={styles.clusterForm}>
        <div>
          <h2>{this.props.cluster?.name || 'New Cluster'}</h2>
          <Form onSubmit={this.handleSubmit}>
            <Form.Group controlId="clusterForm.ControlClusterName">
              <Form.Label>Cluster Name</Form.Label>
              <Form.Control type="text" name="name" placeholder="Cluster name" defaultValue={this.props.cluster?.name} onChange={this.handleChange} />
            </Form.Group>
            <Form.Group controlId="clusterForm.ControlClusterAccessInstance">
              <Form.Label>Access Instance</Form.Label>
              <Form.Control name="accessInstance" as="select" defaultValue={this.props.cluster?.accessInstance !== undefined ? JSON.stringify(this.props.cluster?.accessInstance) : "please-select"} onChange={this.handleChange}>
                { this.generateSelectOptions() }
              </Form.Control>
            </Form.Group>
            <Form.Group controlId="clusterForm.ControlClusterPort">
              <Form.Label>Port</Form.Label>
              <Form.Control type="number" name="targetPort" placeholder="Port" defaultValue={this.props.cluster?.targetPort} onChange={this.handleChange} />
            </Form.Group>
            <Form.Group controlId="clusterForm.ControlClusterPath">
              <Form.Label>Path</Form.Label>
              <Form.Control type="text" name="targetPath" placeholder="Path" defaultValue={this.props.cluster?.targetPath} onChange={this.handleChange} />
            </Form.Group>
            <InstanceList 
              onRowClick={this.handleSelectInstance}
              selected={this.state.formControls.instances}
              updateInstanceInfos={this.updateInstanceInfos}
            />
            <Form.Row className={styles.formRow}>
              <Button className={styles.button} variant="primary" type="submit">
                  {this.props.submitButtonName}
                </Button>
              <Button className={styles.button} variant="secondary" onClick={this.handleCancel}>
                Close
              </Button>
          </Form.Row>
          </Form>
        </div>
      </div>
    );
  }
}

export default withRouter(ClusterForm)