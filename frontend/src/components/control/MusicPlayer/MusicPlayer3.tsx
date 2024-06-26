import { useState, useRef, SetStateAction, useEffect } from 'react';
import PauseIcon from 'src/asset/images/icon/pause.svg';
import NextIcon from 'src/asset/images/icon/next.svg';
import PrevIcon from 'src/asset/images/icon/prev.svg';
import PlayIcon from 'src/asset/images/icon/play.svg';
import ReactPlayer from 'react-player';
import styled, { keyframes } from 'styled-components';

const PlayerWrapper = styled.div`
  box-sizing: border-box;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  line-height: 1;
  background-color: white;
  border-radius: 20px;
  width: 100%;
  padding: 30px;
  margin-top: 10px;
  margin-bottom: 40px;
  box-shadow: 0 5px 10px rgba(0, 0, 0, 0.1);
  line-height: 40px;
  height: 300px;
`;

const ControlWrapper = styled.div`
  width: 58%;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  padding: 10px;
`;

const Controls = styled.div`
  width: 100%;
  display: flex;
  justify-content: space-between;
  flex-direction: row;
  align-items: center;
  margin-top: 10px;
`;

const Button = styled.button`
  color: #262626;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: transparent;
  border: none;
  cursor: pointer;
  &:focus {
    outline: none;
  }
  &:hover {
    opacity: 0.8;
  }
`;

const ProgressWrapper = styled.div`
  width: 100%;
  height: 10px;
  background-color: #CDCDCD;
  border-radius: 60px;
  cursor: pointer;
  margin: 20px;
`;

const ProgressBar = styled.div`
  height: 100%;
  border-radius: 60px;
  background-color: #262626;
`;

const Thumbnail = styled.img`
  width: 35%; 
  height: auto;
  border-radius: 4px;
  margin-right: 5px; 
`;

const TitleContainer = styled.div`
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
`;

const TitleWrapper = styled.div<{ length: number }>`
  display: inline-block;
  padding: 10px;
  font-size: 36px;
  font-weight: bold;
  
  ${props => props.length >= 20 && `
    animation: slideText 10s linear infinite;

    @keyframes slideText {
      from {
        transform: translateX(5%);
      }
      to {
        transform: translateX(-100%);
      }
    }
  `}
`;

const tape = keyframes`
  0% {
    transform: rotate(0deg) scale(0.4);
  }
  100% {
    transform: rotate(-360deg) scale(0.4);
  }
`;

const Spinner = styled.span`
  margin: auto;
  width: 100px;
  height: 30px;
  overflow: hidden;
  position: relative;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 5px;
  box-shadow: 0px 35px 0 -5px #aaa, 
              0 -5px 0 0px #ddd, 
              0 -25px 0 -5px #fff,
              -25px -30px 0 0px #ddd, 
              -25px 30px 0 0px #ddd, 
              25px -30px 0 0px #ddd,
              25px 30px 0 0px #ddd, 
              20px 10px 0 5px #ddd, 
              20px -10px 0 5px #ddd,
              -20px -10px 0 5px #ddd, 
              -20px 10px 0 5px #ddd;

  &::after,
  &::before {
    content: "";
    border-radius: 100%;
    width: 35px;
    height: 35px;
    display: block;
    position: absolute;
    border: 4px dashed #fff;
    bottom: -4px;
    transform: rotate(0deg);
    box-sizing: border-box;
    animation: ${tape} 4s linear infinite;
  }

  &::before {
    right: 0;
    box-shadow: 0 0 0 4px #fff, 0 0 0 34px #000;
  }

  &::after {
    left: 0;
    box-shadow: 0 0 0 4px #fff, 0 0 0 65px #000;
  }
`;

const AudioVideoPlayer = ( props: MusicListProps ) => {

  const { playList } = props;
  
  const [playing, setPlaying] = useState( true );
  const [trackIndex, setTrackIndex] = useState(0);
  const [played, setPlayed] = useState(0);
  const [ recommends, setRecommends ] = useState([])
  const playerRef = useRef( null );
  const [ Title, setTitle ] = useState('');
  const [ Singer, setSinger ] = useState('');

  const togglePlayPause = () => setPlaying( !playing );
  const playPreviousTrack = () => setTrackIndex(( prevIndex ) => ( prevIndex === 0 ? recommends.length - 1 : prevIndex - 1 ));
  const playNextTrack = () => setTrackIndex(( prevIndex ) => ( prevIndex === recommends.length - 1 ? 0 : prevIndex + 1 ));
  const handleProgress = ( state: { played: SetStateAction<number>; }) => {
    setPlayed( state.played );
  };

  const seekTo = ( e ) => {
    const progressContainerRect = e.currentTarget.getBoundingClientRect();
    const clickPosition = ( e.clientX - progressContainerRect.left ) / progressContainerRect.width;
    const seekTime = playerRef.current.getDuration() * clickPosition;
    playerRef.current.seekTo( seekTime, 'seconds' );
    setPlayed( clickPosition );
  };



  useEffect(() => {
    // temp에 정보 담고
    console.log(playList)
    const temp = [];
    if (playList && Array.isArray(playList)) {
      playList.forEach((e) => {
        if ('uri' in e && 'song_name' in e && 'artist_name_basket' in e) {
          temp.push({
            uri: 'https://www.youtube.com/watch?v=' + e.uri,
            songName: e.song_name,
            artist: Array.isArray(e.artist_name_basket) ? e.artist_name_basket.join(', ') : e.artist_name_basket
          });
        }
      });
    }
    // console.log(temp)
    setRecommends(temp);
  }, [playList]);


  useEffect(() => {
    if ( recommends && recommends.length > 0 && recommends[trackIndex] ) {
      // 현재 트랙 제목 가수 설정
      const currentTrack = recommends[trackIndex];
      setTitle( currentTrack.songName ); 
      setSinger( currentTrack.artist );
    }
  }, [trackIndex, recommends]); 

  const getThumbnailUrl = ( url:string ) => {
    //예외처리
    if (!url) return "";
    const videoId = url.split('v=')[1];
    return `https://img.youtube.com/vi/${ videoId }/default.jpg`;
  }

  return (
    <PlayerWrapper>
        {(!playList?.length) && (
          <Spinner />
        )}
        {(playList?.length) && (
          <>
            <Thumbnail src={ getThumbnailUrl( recommends[ trackIndex ]?.uri)} alt="Video thumbnail" />
            <ControlWrapper>
                <TitleContainer>
                  <TitleWrapper length={ Title.length }>{Title || 'title'}</TitleWrapper>
                  <div style={{ fontSize: '28px', padding: '10px'  }}>{Singer || 'singer'}</div>
                </TitleContainer>
                <ProgressWrapper onClick={ seekTo }>
                    <ProgressBar style={{ width: `${played * 100}%` }} />
                </ProgressWrapper>
                <Controls>
                    <Button onClick={ playPreviousTrack }><img src={ PrevIcon } alt='prev'/></Button>
                    <Button onClick={ togglePlayPause }>
                        {playing ? <img src={ PauseIcon } alt='pause'/> : <img src={ PlayIcon } alt='play'/> }
                    </Button>
                    <Button onClick={ playNextTrack }> <img src={ NextIcon } alt='next'/></Button>
                </Controls>
            </ControlWrapper>
          </>
        )}

        <ReactPlayer
        ref={ playerRef }
        url={ recommends[ trackIndex ]?.uri }
        playing={ playing }
        onProgress={ handleProgress }
        controls
        style={{ display: 'none'}}
        onEnded={ playNextTrack }
      />
    </PlayerWrapper>
    
  );
};

export default AudioVideoPlayer;


type MusicListProps = {
  playList: Array<Object>,
}
