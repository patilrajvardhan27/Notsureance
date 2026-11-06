package com.notsureance.claims.claim;

import com.notsureance.claims.common.BaseEntity;
import com.notsureance.claims.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "claim_events")
public class ClaimEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id")
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ClaimEventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ClaimStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private ClaimStatus toStatus;

    @Column(length = 2000)
    private String note;

    protected ClaimEvent() {
    }

    public ClaimEvent(Claim claim, AppUser actor, ClaimEventType type, ClaimStatus fromStatus, ClaimStatus toStatus,
                      String note) {
        this.claim = claim;
        this.actor = actor;
        this.type = type;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
    }

    public AppUser getActor() {
        return actor;
    }

    public ClaimEventType getType() {
        return type;
    }

    public ClaimStatus getFromStatus() {
        return fromStatus;
    }

    public ClaimStatus getToStatus() {
        return toStatus;
    }

    public String getNote() {
        return note;
    }
}
