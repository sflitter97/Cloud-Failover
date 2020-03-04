import React from 'react';
import styles from './InstanceList.module.css';
import Instance from '../../interfaces/Instance';
import { API_METHODS as API } from '../../api_methods';
import 'react-bootstrap-table-next/dist/react-bootstrap-table2.min.css';
import BootstrapTable from 'react-bootstrap-table-next';

export interface InstanceListProps {
}

interface InstanceListState {
  instances: Array<Instance>;
}

export class InstanceList extends React.Component<InstanceListProps, InstanceListState> {
  constructor(props: InstanceListProps) {
    super(props);
    this.state = {
      instances: Array<Instance>()
    };
  }

  componentDidMount() {
    // fetch instance list
    fetch(API.BASE + API.INSTANCES)
    .then(res => res.json())
    // fetch details of each instance individually
    .then(data => {
      return data._embedded.instances.map((instance: { _links: { self: { href: RequestInfo; }; }; }) =>
				fetch(instance._links.self.href)
		  );
    })
    // gather the individual response promises into one
    .then(instancePromises => Promise.all<Response>(instancePromises))
    // convert each to json
    .then(instancePromises => {
      return instancePromises.map((value) => value.json())
    })
    // gather the individual json promises into one
    .then(instanceJsonPromises => Promise.all<Instance>(instanceJsonPromises))
    .then(instanceJson => {
      this.setState({
        instances: instanceJson
      })
    })
    .catch(console.log);
  }

  handleRowClick(idx: number) {
    console.log(idx);
    if(idx >= 0 && idx < this.state.instances.length) {
      this.setState({
      });
    }
  }

  handleModalClose = () => {
    this.setState({
    });
  }

  render() {
    const columns = [
      {
        dataField: 'handle',
        hidden: true
      },
      {
        dataField: 'provider',
        text: 'Provider',
        sort: true
      },
      {
        dataField: 'name',
        text: 'Name',
        sort: true
      },
      {
        dataField: 'type',
        text: 'Type',
      },
      {
        dataField: 'state',
        text: 'State'
      },
      {
        dataField: 'host',
        text: 'Host',
        formatter: (cell: string, row: any, rowIndex: number, formatExtraData: any) => {
          return <a href={cell} rel="noopener noreferrer" target="_blank">{cell}</a>;
        }
      }
    ]
    const selectRow = {
      mode: 'checkbox',
      onSelect: (row: any, isSelect: any, rowIndex: any, e: any) => {
        if(e.target.tagName === 'A') {
          return false;
        }
        console.log(row)
      },
      clickToSelect: true,
      hideSelectColumn: true
    };
    return (
      <div className={styles.InstanceList}>
          <h2>Instances</h2>
          <BootstrapTable 
            keyField='name'
            data={this.state.instances.map(instance => {
              console.log(instance);
              return {
                handle: instance.handle,
                provider: instance.provider,
                name: instance.name,
                type: instance.type,
                state: instance.state,
                host: instance.host
              }
            })} 
            columns={columns}
            bootstrap4={true}
            bordered={false}
            hover={true}
            selectRow={selectRow}/>
      </div>
    );
  }
}
