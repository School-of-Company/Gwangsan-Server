-- Tracks which side (buyer or seller) initiated a trade-complete request,
-- so the other side is the only one allowed to confirm it.
-- Existing rows were always seller-initiated under the previous logic, so default to TRUE.
ALTER TABLE tbl_trade_complete
    ADD COLUMN requested_by_seller BOOLEAN NOT NULL DEFAULT TRUE;
