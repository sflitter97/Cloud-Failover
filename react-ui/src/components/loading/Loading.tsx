import React from 'react';
import { FunctionComponent } from 'react'
import styles from './Loading.module.css'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faSpinner } from '@fortawesome/free-solid-svg-icons'

interface LoadingProps {
    iconSize?: "xs" | "lg" | "sm" | "1x" | "2x" | "3x" | "4x" | "5x" | "6x" | "7x" | "8x" | "9x" | "10x",
    fontSize?: string
}

export const Loading: FunctionComponent<LoadingProps> = (props: LoadingProps) => {
    const fontSize = {fontSize: props.fontSize || "1rem"}
    return <div className={styles.loading}>
        <FontAwesomeIcon icon={faSpinner} size={props.iconSize || "lg"} spin />
        <p style={fontSize}>Loading...</p>
    </div>
}