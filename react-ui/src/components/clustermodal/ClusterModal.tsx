import React from 'react';
import Modal from 'react-bootstrap/Modal';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import styles from './ClusterModal.module.css'
import Cluster from '../../interfaces/Cluster'
import ClusterUpdateDto from '../../interfaces/ClusterUpdateDto'

export interface ClusterModalProps {
  cluster: Cluster;
  accessApi: string;
  show: boolean;
  handleClose: any;
  handleFormSubmit: any;
}

interface ClusterModalState {
  formControls: {
    name: { value: string },
    instances: { value: Array<any> },
    targetPort: { value: number },
    targetPath: { value: string }
  }
}

export class ClusterModal extends React.Component<ClusterModalProps, ClusterModalState> {
  constructor(props: ClusterModalProps) {
    super(props);
    this.state = {
      formControls: {
        name: {value: props.cluster.name},
        instances: { value: props.cluster.instances },
        targetPort: { value: props.cluster.targetPort },
        targetPath: { value: props.cluster.targetPath }
      }
    }
  }

  handleChange = (e: React.FormEvent<HTMLInputElement>) => {
    //e.persist();
    console.log(e);
    // this.setState(state => {
    //   state.formControls[e.target.name] = e.target.value;

    //   return state
    // });
    const name = e.currentTarget.name as "name";
    const value = e.currentTarget.value;

    this.setState({
      formControls: {
        ...this.state.formControls,
        [name]: {
          ...this.state.formControls[name],
          value
        }
      }
    })
  }

  handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    let cluster: ClusterUpdateDto = {
      name: this.props.cluster.name === this.state.formControls.name.value ? undefined : this.state.formControls.name.value,
      instances: this.props.cluster.instances === this.state.formControls.instances.value ? undefined : this.state.formControls.instances.value,
      targetPort: this.props.cluster.targetPort === this.state.formControls.targetPort.value ? undefined : this.state.formControls.targetPort.value,
      targetPath: this.props.cluster.targetPath === this.state.formControls.targetPath.value ? undefined : this.state.formControls.targetPath.value
    }
    this.props.handleFormSubmit(cluster);
  }

  render() {
    return (
      <div className={styles.clusterModal}>
        <Modal show={true} onHide={this.props.handleClose} size="lg">
          <Modal.Header closeButton>
          <Modal.Title>{this.props.cluster.name}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Form onSubmit={this.handleSubmit}>
              <Form.Group controlId="clusterEditForm.ControlClusterName">
                <Form.Label>Cluster Name</Form.Label>
                <Form.Control type="text" name="name" defaultValue={this.props.cluster.name} onChange={this.handleChange} />
              </Form.Group>
              <Form.Group controlId="exampleForm.ControlSelect1">
                <Form.Label>Example select</Form.Label>
                <Form.Control as="select">
                  <option>1</option>
                  <option>2</option>
                  <option>3</option>
                  <option>4</option>
                  <option>5</option>
                </Form.Control>
              </Form.Group>
              <Form.Group controlId="exampleForm.ControlSelect2">
                <Form.Label>Example multiple select</Form.Label>
                <Form.Control as="select" multiple>
                  <option>1</option>
                  <option>2</option>
                  <option>3</option>
                  <option>4</option>
                  <option>5</option>
                </Form.Control>
              </Form.Group>
              <Form.Group controlId="exampleForm.ControlTextarea1">
                <Form.Label>Example textarea</Form.Label>
                <Form.Control as="textarea" rows="3" />
              </Form.Group>
              <Button variant="primary" type="submit">
                Submit
              </Button>
            </Form>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={this.props.handleClose}>
              Close
            </Button>
          </Modal.Footer>
        </Modal>
      </div>
    );
  }
}
