import InstanceState from './InstanceState';

export type Instance = {
  provider: string,
  name: string,
  type: string,
  state: InstanceState,
  handle: {},
  host: string
}

export default Instance