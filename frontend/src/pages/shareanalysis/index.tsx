import { Drawer } from "src/components/control/Drawer";
import { Button } from "src/components/control/Button";
import { Header } from "src/components/control/Header";
import { LeftIcon, RightIcon } from 'src/components'
import { getDiaryEmotion } from "./api";
import { addMonths, format, subMonths } from 'date-fns'
import { useQuery } from 'react-query'
import { getMember } from "./api";
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    ArcElement,
    PointElement,
    LineElement,
    Tooltip,
} from "chart.js";
import { useEffect, useState } from "react";
import { Line } from "react-chartjs-2";
import { BottomNavi } from "src/components/control/BottomNavi";
import useStore from 'src/store'
import useDate from 'src/util/date'
import Chart from "./components/Chart/Chart";
import styled from 'styled-components';

ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    ArcElement,
    Tooltip,
);

const BoxWrapper = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: center; 
  align-items: center; 
  gap: 20px; 
  margin-top: 0px;
  border-radius: 4px;
  overflow: hidden;
  width: 100%;
  box-sizing: border-box; 
`;

const Title = styled.div`
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 10px;
    justify-content: flex-start;
`

const TitleWrapper = styled.div`
font-family: 'IMHyeMin', sans-serif;
font-weight: bold;
display: flex;
justify-content: center;
align-items: center;
width: 70%;
font-size: 16px;
text-align: left; 
margin-left: 20px; 
margin-bottom: 0px; 
`;

const CalendarHeaderMiddleWrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 30px; 
  width: 100%;
  margin-top: 10px;
  flex: 1;
`


const ShareAnalysis = () => {

    const [ userName, setUserName ] = useState( "" );
    const { currentMonth, setCurrentMonth } = useDate()
    
    const { data: memberData, refetch: refetchMember } = useQuery(['memberData'], getMember, {
        onSuccess: ( res ) => {
          setUserName( res.data.nickName );
          console.log( res.data );
        }
    });

    const movePrevMonth = () =>{ setCurrentMonth( subMonths( currentMonth, 1) )}
    const moveNextMonth = () => { setCurrentMonth( addMonths( currentMonth, 1) ) }
    const today = format(currentMonth, 'yyyy-MM-01')

    const getEmotion = () =>{
        getDiaryEmotion({ diaryId:17, date:today }).then(( res )=>{
            console.log("111111")
            console.log( res.data )
        })
    }

    useEffect(() => {
        refetchMember();
        getEmotion();
    },[ currentMonth ]); 

    return(
        <>
                <Header>
                    <Drawer></Drawer>
                    <Button></Button>
                    <Button></Button>
                </Header>
                <CalendarHeaderMiddleWrapper>
                    <LeftIcon size= { 20 } onClick={ movePrevMonth }/>
                    <Title>{ format( currentMonth, 'yyyy' )}년 { format( currentMonth, 'M' )}월</Title>
                    <RightIcon size= { 20 } onClick={ moveNextMonth }/>
                </CalendarHeaderMiddleWrapper>
                    <div style={{ width: '100%', marginBottom: '0px' }}>
                        { userName && <TitleWrapper> { userName }님이 { format( currentMonth, 'M' )}월에 느낀 “감정 통계” 를 비교해보아요! </TitleWrapper>}
                    </div>
                    <BoxWrapper>
                        <Chart/>
                    </BoxWrapper>
                    { userName && <TitleWrapper> { userName }님과 친구의 { format( currentMonth, 'M' )}월에 느낀 “감정 통계” 를 비교해보아요! </TitleWrapper>}
                    <BoxWrapper>
                        <Chart/>
                    </BoxWrapper>
                    <BottomNavi/>
                
        </>
    );

}

export default ShareAnalysis;