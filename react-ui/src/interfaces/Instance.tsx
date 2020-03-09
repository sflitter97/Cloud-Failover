import InstanceState from './InstanceState';

export type Instance = {
  _links: { [key: string]: {'href': string}}
  provider: string,
  name: string,
  type: string,
  state: InstanceState,
  handle: {},
  host: string
}

export default Instance