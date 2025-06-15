package com.ecommerce.app.model;

import org.hibernate.envers.Audited;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
@Audited
public abstract class AuditableEntity extends BaseEntity {
}
