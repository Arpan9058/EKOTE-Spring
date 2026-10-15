package com.ekote.service;

import com.ekote.entities.MovementType;

public record StockChanged(MovementType operation, int delta) {}
