import { useNavigate } from "react-router-dom";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faArrowLeft, faBars, faTrash, faUser, faHouse } from '@fortawesome/free-solid-svg-icons';
import { faCircleCheck, faBell } from '@fortawesome/free-regular-svg-icons';
import styled from 'styled-components';

const ButtonWrapper = styled.button`
    border: 0;
    background-color: transparent; 
    transition: background-color 0.3s, color 0.3s;
    &:hover {
    background-color: #e0e0e0;
    }
`;

const iconPaths = {
    back: faArrowLeft,
    hamburger: faBars,
    check: faCircleCheck, 
    trash: faTrash,
    bell: faBell, 
    user: faUser,
    home: faHouse,
}


const Button = (props: ButtonProps) => {

    const {btOn, path, btType, name, onButtonClick } = props

    const iconSrc = btType ? iconPaths[btType] : undefined;

    const navigator = useNavigate();
    
    const eventHandle = () => {
        if ( btType === 'hamburger' ) {
            if( typeof onButtonClick === 'function' ) {
              onButtonClick();
            }
        }
        else if ( btType === 'back' ) {
            navigator(-1);
        }
        else {
            if( path ) {
              navigator( path );
            }
          }
        
    }

    return(
        <ButtonWrapper onClick={ eventHandle }>
            {iconSrc && <FontAwesomeIcon icon={ iconSrc } />} 
        </ButtonWrapper>

    );
}

type ButtonProps = {
    btOn?: boolean;
    path?: string;
    btType?: keyof typeof iconPaths;
    name?: String;
    onButtonClick?: () => void;
}

export default Button;