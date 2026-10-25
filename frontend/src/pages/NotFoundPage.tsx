import React from "react";
import { Link } from "react-router-dom";
import { Card } from "../components/common/Card";
import { Button } from "../components/common/Button";
import { TaskIllustration } from "../components/common/TaskIllustration";
import { Home } from "lucide-react";

export const NotFoundPage: React.FC = () => {
  return (
    <div style={{ maxWidth: "480px", margin: "4rem auto" }}>
      <Card style={{ padding: "2rem 2rem 2.25rem", textAlign: "center" }}>
        <TaskIllustration variant="notfound" />

        <span
          style={{
            display: "inline-block",
            padding: "0.25rem 0.75rem",
            borderRadius: "var(--radius-badge)",
            backgroundColor: "var(--primary-light)",
            color: "var(--primary)",
            fontSize: "0.75rem",
            fontWeight: 600,
          }}
        >
          Error 404
        </span>

        <h1
          style={{
            fontSize: "1.5rem",
            fontWeight: 700,
            letterSpacing: "-0.02em",
            color: "var(--text-main)",
            margin: "0.85rem 0 0.5rem",
          }}
        >
          This Page could not be found
        </h1>
        <p
          style={{
            color: "var(--text-muted)",
            marginBottom: "1.75rem",
            fontSize: "0.95rem",
          }}
        >
          The page you requested does not exist or may have been moved or
          deleted.
        </p>

        <Link to="/tasks">
          <Button variant="primary" size="md" icon={<Home size={18} />} style={{ width: "100%" }}>
            Back to Task Pool
          </Button>
        </Link>
      </Card>
    </div>
  );
};