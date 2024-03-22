import styled from "styled-components"
import useStore from "../store"
import TodoList from "./TodoList"
import { LeftIcon, RightIcon } from 'src/components'
import { format } from "date-fns"
import Todo from "../todo"
import { useState } from "react"
import { useNavigate } from "react-router-dom"

const MyModalWrapper = styled.div`
  width: 80%;
  display: flex;
  flex-direction: column;
  height: 100%;
  margin-left: 10%;
  overflow: auto;
`
const ModalHeaderWrapper = styled.div`
  display: flex;
  margin-left: 1%;
  margin-top: 3%;
  margin-bottom: 2%;
  align-items: flex-end;
`

const PlusButton = styled.button`
  border: 1px solid;
  height: 10%;
  width: 100%;
  background-color: #fff;
  border-radius: 10px;
  font-size: 1.5rem;
  cursor: pointer;
`
const TodoHeaderWrapper = styled.div`
  display: flex;
  width: 100%;
  justify-content: space-between;
  margin-top: 2%;
`

const TodoTitle = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 1%;
`

const TodoWrapper = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`

const ColorBoxWrapper = styled.div`
  border: 1px solid;
  border-radius: 10px;
  height: 20%;
  display: flex;
  justify-content: start;
  gap: 1rem;
  padding-top: 1.2rem;
  padding-left: 1rem;
`

const ColorBox = styled.button<{ color: string }>`
  border: none;
  border-radius: 100%;
  height: 60%;
  width: 5%;
  cursor: pointer;
  background-color: ${ ( props ) => props.color };
`


const MyModal = () => {

  const navigator = useNavigate()

  const [ modalContent, setModalContent ] = useState(true);

  const { clickDate } = useStore()

  const createTodo = () =>{
    setModalContent(!modalContent)
  }

  const moveBack = () =>{
    setModalContent(!modalContent)
  }
  const RegistTodo = () =>{
    navigator( '/calendar' )
  }

  return(
    <MyModalWrapper>
      { modalContent &&
      <>
      <ModalHeaderWrapper>
        <div style={{ fontSize:'30px'}}>일정</div>
        <div style={{ fontSize: '20px', marginLeft: '2%' }}> { format(clickDate, 'yyyy-MM-dd' ) }</div>
      </ModalHeaderWrapper>
        <PlusButton onClick={ createTodo }>+</PlusButton>
        <TodoList/>
      </>
      }{
        !modalContent &&
        <Todo/>
      }
    </MyModalWrapper>
  )
}

export default MyModal