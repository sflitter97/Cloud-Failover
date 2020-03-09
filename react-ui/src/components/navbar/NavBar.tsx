import React from 'react';
import { Link } from 'react-router-dom';
import Navbar from 'react-bootstrap/Navbar';
import Nav from 'react-bootstrap/Nav';
import styles from './NavBar.module.css';

export interface NavBarProps {
}

export const NavBar: React.FC<NavBarProps> = (props: NavBarProps) => {
  return <Navbar variant="dark" className={styles.navBar} expand="lg">
    <Navbar.Brand as={Link} to="/clusters">MultiCloud Failover</Navbar.Brand>
    <Navbar.Toggle aria-controls="basic-navbar-nav" />
    <Navbar.Collapse id="basic-navbar-nav">
      <Nav className="mr-auto">
        <Nav.Link as={Link} to="/clusters">Clusters</Nav.Link>
        <Nav.Link as={Link} to="/instances">Instances</Nav.Link>
      </Nav>
    </Navbar.Collapse>
  </Navbar>;
}
