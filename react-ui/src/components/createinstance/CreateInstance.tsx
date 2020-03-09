import React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom'
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import styles from './CreateInstance.module.css';
import {API_METHODS as API } from '../../api_methods';

export interface CreateInstanceProps extends RouteComponentProps {
}

interface CreateInstanceState {
  formControls: {
    provider: string,
    name: string,
    type: string,
    imageId: string,
    region: string
  };
  providers: Array<string>;
}

class CreateInstance extends React.Component<CreateInstanceProps, CreateInstanceState> {
  constructor(props: CreateInstanceProps) {
    super(props);
    this.state = {
      formControls: {
        provider: "AWS",
        name: "",
        type: "",
        imageId: "",
        region: ""
      },
      providers: ["AWS", "GCP", "Azure"]
    }
  }

  handleChange = (e: React.FormEvent<HTMLInputElement>) => {
    // e.persist()
    // console.log(e.currentTarget);
    this.setState({
      formControls: {
        ...this.state.formControls,
        [e.currentTarget.name]: e.currentTarget.value
      }
    });
  }

  handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    fetch(API.BASE + API.INSTANCES, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(this.state.formControls)
    })
    .then(res => res.json)
    .then(data => {
      this.props.history.push("/instances");
    })
  }

  handleCancel = () => {
    this.props.history.goBack();
  }

  render() {
    return (
      <div className={styles.createInstance}>
        <div>
          <h2>New Instance</h2>
          <Form onSubmit={this.handleSubmit}>
          <Form.Group controlId="createInstance.ControlInstanceProvider">
              <Form.Label>Provider</Form.Label>
              <Form.Control name="provider" as="select" defaultValue={"AWS"} onChange={this.handleChange}>
                {this.state.providers.map(provider => {
                  return <option key={provider} value={provider.toUpperCase()}>{provider}</option>
                })}
              </Form.Control>
            </Form.Group>
            <Form.Group controlId="createInstance.ControlInstanceName">
              <Form.Label>Instance Name</Form.Label>
              <Form.Control type="text" name="name" placeholder="Instance name" onChange={this.handleChange} />
            </Form.Group>
            <Form.Group controlId="createInstance.ControlInstanceType">
              <Form.Label>Instance Type</Form.Label>
              <Form.Control type="text" name="type" placeholder="Instance Type" onChange={this.handleChange} />
            </Form.Group>
            <Form.Group controlId="createInstance.ControlInstanceImageId">
              <Form.Label>Instance Image ID</Form.Label>
              <Form.Control type="text" name="imageId" placeholder="Instance Image ID" onChange={this.handleChange} />
            </Form.Group>
            <Form.Group controlId="createInstance.ControlInstanceRegion">
              <Form.Label>Instance Region</Form.Label>
              <Form.Control type="text" name="region" placeholder="Instance Region" onChange={this.handleChange} />
            </Form.Group>
            <Form.Row className={styles.formRow}>
              <Button className={styles.button} variant="primary" type="submit">
                  Create
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

export default withRouter(CreateInstance)