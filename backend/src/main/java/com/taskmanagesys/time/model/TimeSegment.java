package com.taskmanagesys.time.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "time_segments")
public class TimeSegment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="session_id", nullable=false) private Long sessionId;
    @Column(name="started_at", nullable=false) private Instant startedAt;
    @Column(name="ended_at", nullable=false) private Instant endedAt;
    @Column(name="worked_seconds", nullable=false) private long workedSeconds;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getSessionId(){return sessionId;} public void setSessionId(Long v){sessionId=v;}
    public Instant getStartedAt(){return startedAt;} public void setStartedAt(Instant v){startedAt=v;}
    public Instant getEndedAt(){return endedAt;} public void setEndedAt(Instant v){endedAt=v;}
    public long getWorkedSeconds(){return workedSeconds;} public void setWorkedSeconds(long v){workedSeconds=v;}
}
