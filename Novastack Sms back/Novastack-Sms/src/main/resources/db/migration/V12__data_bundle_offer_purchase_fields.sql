-- Extra Safaricom Dynamic Offers fields needed for purchase fulfilment.
ALTER TABLE data_bundle_offers
    ADD COLUMN account_id VARCHAR(64) NULL AFTER offer_id,
    ADD COLUMN resource_amount VARCHAR(40) NULL AFTER amount,
    ADD COLUMN offer_source VARCHAR(40) NULL AFTER category,
    ADD COLUMN parent_offer_id VARCHAR(64) NULL AFTER offer_source;
