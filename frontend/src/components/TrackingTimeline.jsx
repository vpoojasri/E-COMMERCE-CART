import React from 'react';

const STAGES = [
  { key: 'PLACED', label: 'Order Placed', icon: '📝' },
  { key: 'CONFIRMED', label: 'Confirmed', icon: '✅' },
  { key: 'PACKED', label: 'Packed', icon: '📦' },
  { key: 'SHIPPED', label: 'Shipped', icon: '🚚' },
  { key: 'OUT_FOR_DELIVERY', label: 'Out for Delivery', icon: '🛵' },
  { key: 'DELIVERED', label: 'Delivered', icon: '🏠' },
];

export function StatusBadge({ status }) {
  if (!status) return null;
  const cls = `status-badge status-${status.toLowerCase()}`;
  return <span className={cls}>{status.replace(/_/g, ' ')}</span>;
}

export function TrackingTimeline({ currentStatus, trackingHistory = [], estimatedDeliveryDate }) {
  const isCancelled = currentStatus === 'CANCELLED';
  const currentIdx = STAGES.findIndex((s) => s.key === currentStatus);

  // Map stage key to earliest matching history entry
  const historyByStage = {};
  trackingHistory.forEach((h) => {
    if (!historyByStage[h.status]) {
      historyByStage[h.status] = h;
    }
  });

  return (
    <div className="tracking-card">
      <div className="tracking-header-bar">
        <div>
          <h4>Live Order Lifecycle Progression</h4>
          <p className="muted small">
            Connected to MySQL/Hibernate tracking history records
          </p>
        </div>
        <div className="tracking-eta-box">
          <span className="muted small">Current Status</span>
          <StatusBadge status={currentStatus} />
          {!isCancelled && estimatedDeliveryDate && (
            <div className="eta-pill">
              📅 Est. Delivery: <strong>{estimatedDeliveryDate}</strong>
            </div>
          )}
        </div>
      </div>

      {isCancelled ? (
        <div className="cancelled-banner">
          <strong>⚠️ Order Cancelled</strong>
          <p>
            This order was cancelled and any reserved product inventory has been restored to stock.
          </p>
        </div>
      ) : (
        <div className="stepper-horizontal">
          {STAGES.map((stage, index) => {
            const isCompleted = index <= currentIdx;
            const isCurrent = index === currentIdx;
            const eventInfo = historyByStage[stage.key];
            return (
              <div
                key={stage.key}
                className={`stepper-step ${isCompleted ? 'completed' : ''} ${isCurrent ? 'active' : ''}`}
              >
                <div className="step-circle">{isCompleted ? stage.icon : index + 1}</div>
                <div className="step-label">{stage.label}</div>
                {eventInfo && (
                  <div className="step-time">
                    {new Date(eventInfo.updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      <div className="history-log-section">
        <h5>Detailed Audit &amp; Tracking Event Log ({trackingHistory.length} Events)</h5>
        <div className="vertical-timeline">
          {trackingHistory.map((item, idx) => (
            <div key={item.id || idx} className="timeline-entry">
              <div className="timeline-dot" />
              <div className="timeline-content">
                <div className="timeline-row-top">
                  <StatusBadge status={item.status} />
                  <span className="timeline-timestamp">
                    {new Date(item.updatedAt).toLocaleString()}
                  </span>
                </div>
                <p className="timeline-desc">{item.description}</p>
                <div className="timeline-meta">
                  {item.locationNote && <span>📍 {item.locationNote}</span>}
                  <span>👤 Updated by: {item.updatedBy}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

