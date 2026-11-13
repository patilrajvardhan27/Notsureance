-- Two farms with different owners shared a phone, a payout account and a repair shop, which is the ring
-- the graph is meant to surface. Sunfield and Prairie Bend sat one and two hops away.
UPDATE policies SET holder_phone = '+13085550147', payout_account_hash = 'a3f1c9e07b2d4458', annual_premium = 18400
WHERE policy_number = 'NSF-100232';
UPDATE policies SET holder_phone = '+13085550147', payout_account_hash = 'a3f1c9e07b2d4458', annual_premium = 9600
WHERE policy_number = 'NSF-100234';
UPDATE policies SET holder_phone = '+13305550112', payout_account_hash = '5b7e21d9c0aa3f61', annual_premium = 6100
WHERE policy_number = 'NSF-100233';
UPDATE policies SET holder_phone = '+13305550112', payout_account_hash = 'e4c8b17a92f05d3e', annual_premium = 14200
WHERE policy_number = 'NSF-100231';
UPDATE policies SET holder_phone = '+18705550183', payout_account_hash = '0d9a6f3b51e7c2a8', annual_premium = 16900
WHERE policy_number = 'NSF-100235';
UPDATE policies SET holder_phone = '+17155550126', payout_account_hash = '7c25e8f04ab91d6e', annual_premium = 7300
WHERE policy_number = 'NSF-100236';

UPDATE claims SET repair_dealer = 'Prairie Implement Repair' WHERE claim_number IN ('FEQ-2026-F1R3S9K', 'FEQ-2026-THF7Z2W', 'FEQ-2026-VND3K8P');
UPDATE claims SET repair_dealer = 'Lakeshore Ag Service' WHERE claim_number IN ('FEQ-2026-L1GHT55', 'FEQ-2026-MECH8B2');
UPDATE claims SET repair_dealer = 'Heartland Equipment' WHERE claim_number IN ('FEQ-2026-H4ILR7X', 'FEQ-2026-TRN6M3A');
