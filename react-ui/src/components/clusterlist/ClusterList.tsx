import React from 'react';
import { RouteComponentProps, withRouter, Link } from 'react-router-dom';
import styles from './ClusterList.module.css';
import Cluster from '../../interfaces/Cluster';
import { API_METHODS as API } from '../../api_methods';
import 'react-bootstrap-table-next/dist/react-bootstrap-table2.min.css';
import BootstrapTable from 'react-bootstrap-table-next';
import { Loading } from '../loading/Loading'
import Row from 'react-bootstrap/Row';
import Button from 'react-bootstrap/Button';

export interface ClusterListProps extends RouteComponentProps {
  clusters: Array<Cluster>;
  handleUpdateClusters: (c: Array<Cluster>) => void;
}

interface ClusterListState {
  selectedCluster: number;
  showModal: boolean;
}

class ClusterList extends React.Component<ClusterListProps, ClusterListState> {
  constructor(props: ClusterListProps) {
    super(props);
    this.state = {
      selectedCluster: 0,
      showModal: false

    };
  }

  async componentDidMount() {
    // fetch cluster list
    fetch(API.BASE + API.CLUSTERS)
    .then(res => res.json())
    // fetch details of each cluster individually
    .then(data => {
      return data._embedded.clusters.map((cluster: Cluster) =>
				fetch(cluster._links.self.href)
		  );
    })
    // gather the individual response promises into one
    .then(clusterPromises => Promise.all<Response>(clusterPromises))
    // convert each to json
    .then(clusterPromises => {
      return clusterPromises.map((value) => value.json())
    })
    // gather the individual json promises into one
    .then(clusterJsonPromises => Promise.all<Cluster>(clusterJsonPromises))
    .then(clusterJson => {
      this.props.handleUpdateClusters(clusterJson);
      this.setState({
        selectedCluster: 0,
        showModal: false
      })
    })
    .catch(console.log);
  }

  handleRowClick(idx: number) {
    console.log(idx);
    if(idx >= 0 && idx < this.props.clusters.length) {
      this.setState({
        selectedCluster: idx,
        showModal: true
      });
    }
  }

  render() {
    const columns = [
      {
        dataField: 'id',
        hidden: true
      },
      {
        dataField: 'name',
        text: 'Name',
        sort: true
      },
      {
        dataField: 'targetPort',
        text: 'Target Port',
        type: "number"
      },
      {
        dataField: 'targetPath',
        text: 'Target Path'
      },
      {
        dataField: 'instances',
        text: 'Instances',
        type: "number"
      },
      {
        dataField: 'access',
        text: 'Access',
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
        this.props.history.push(`/clusters/${row.id}`)
        console.log(row)
      },
      clickToSelect: true,
      hideSelectColumn: true
    };
    return (
      <div className={styles.clusterList}>
          <Row>
            <h2>Clusters</h2>
            <Link to="/clusters/new" className={styles.createButton}><Button>Create Cluster</Button></Link>
          </Row>
          <BootstrapTable 
            keyField='name'
            data={this.props.clusters.map(cluster => {
              console.log(cluster);
              return {
                id: cluster.id,
                name: cluster.name,
                targetPort: cluster.targetPort,
                targetPath: cluster.targetPath,
                instances: cluster.instances.length,
                access: cluster._links.access.href
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

export default withRouter(ClusterList)