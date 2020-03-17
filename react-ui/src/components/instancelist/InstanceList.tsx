import React from 'react';
import styles from './InstanceList.module.css';
import Instance from '../../interfaces/Instance';
import { API_METHODS as API } from '../../api_methods';
import 'react-bootstrap-table-next/dist/react-bootstrap-table2.min.css';
import BootstrapTable from 'react-bootstrap-table-next';
import InstanceState from '../../interfaces/InstanceState';
import { Loading } from '../loading/Loading';

export interface InstanceListProps {
  onRowClick?: (row: any, isSelect: any) => void
  selected?: Array<{}>
  updateInstanceInfos?: (instanceInfos: Array<Instance>) => void
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
    .then(instanceJsonPromises => Promise.all<{state: string}>(instanceJsonPromises))
    .then(instanceJsonPromises => {
      return instanceJsonPromises.map((value) => {
        return {...value, state: InstanceState.getState(value.state)}
      })
    })
    // gather the individual json promises into one
    .then(instanceJsonPromises => Promise.all<Instance>(instanceJsonPromises as any))
    .then(instanceJson => {
      // instanceJson.forEach(value => {
      //   value.state = InstanceState.getState(value.state)
      // });
      this.setState({
        instances: instanceJson
      });
      if(this.props.updateInstanceInfos !== undefined)
        this.props.updateInstanceInfos(this.state.instances);
    })
    .catch(console.log);
  }

  render() {
    const columns = [
      {
        dataField: 'instance',
        hidden: true,
      },
      {
        dataField: 'handleStr',
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
          let url = "";
          if(cell !== "") {
            url = /http[s]*:\/\//.test(cell) ? cell : "http://" + cell;
          }
          return <a href={url} rel="noopener noreferrer" target="_blank">{url}</a>;
        }
      }
    ]
    const selectRow = {
      mode: 'checkbox',
      onSelect: (row: any, isSelect: boolean, rowIndex: any, e: any) => {
        if(e.target.tagName === 'A') {
          return false;
        }
        console.log(row);
        console.log(isSelect);
        if(this.props.onRowClick !== undefined) {
          this.props.onRowClick(row, isSelect);
        }
      },
      clickToSelect: true,
      hideSelectColumn: true,
      selected: this.props.selected?.map(sValue => JSON.stringify(sValue)),
      classes: "table-primary",
      nonSelectableClasses: "table-dark"
    };
    return (
      <div className={styles.instanceList}>
          <h2>Instances</h2>
          <BootstrapTable 
            keyField='handleStr'
            data={this.state.instances.map(instance => {
              return {
                instance: instance,
                handleStr: JSON.stringify(instance.handle),
                provider: instance.provider,
                name: instance.name,
                type: instance.type,
                state: instance.state.toString(),
                host: instance.host
              }
            })} 
            columns={columns}
            bootstrap4={true}
            bordered={false}
            hover={true}
            selectRow={selectRow}
            noDataIndication={ () => <div className="centerContents"><Loading iconSize="3x" fontSize="2rem" /></div> }
            />
      </div>
    );
  }
}
